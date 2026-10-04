#version 330
uniform sampler2D SceneSampler;
uniform sampler2D GlowSampler;
layout(std140) uniform VisualConfig {
    vec4 Grade;
    vec4 Balance;
    vec4 Effects;
    vec4 Optics;
    vec4 FilterColor;
    vec4 NightColor;
    vec4 DamageColor;
    vec4 VignetteColor;
    vec4 Viewport;
    vec4 Lens;
    vec4 Tonal;
    vec4 Rain;        // wet amount, density, speed, refraction in physical pixels
    vec4 RainOptics;  // pattern, droplet shading, size, placement variation
    vec4 Water;       // submerged amount, refraction in pixels, speed, caustics
    vec4 WaterOptics; // caustic scale, silt
    vec4 Retro;       // strength, pixel size, color levels, dither
    vec4 RetroOptics; // scanlines, line spacing, phosphor
    vec4 RainColor;
    vec4 Silt;        // size, density, drift speed, shape
    vec4 SiltColor;
};
in vec2 texCoord;
out vec4 fragColor;
float hash21(vec2 p) {
    vec3 h=fract(vec3(p.xyx)*.1031); h+=dot(h,h.yzx+33.33);
    return fract((h.x+h.y)*h.z);
}
// Two bounded analytic layers; no textures, loops over particles or persistent rain buffers.
vec3 lensRain(vec2 uv) {
    vec3 drop=vec3(0);
    for(int layer=0;layer<2;layer++) {
        float depth=1.0+float(layer)*.45;
        vec2 p=uv*vec2(Viewport.x/Viewport.y,1)*Rain.y*depth;
        p.y+=Balance.w*Rain.z*(.45+float(layer)*.18);
        p.x+=float(layer)*3.17;
        vec2 cell=floor(p);
        vec2 local=fract(p)-mix(vec2(.5),vec2(.28+hash21(cell)*.44,.3+hash21(cell+19.0)*.4),RainOptics.w);
        vec2 radii=RainOptics.x>.5 && RainOptics.x<1.5?vec2(.09,.32):RainOptics.x>1.5?vec2(.1,.12):vec2(.16,.22);
        vec2 normal=local/(radii*RainOptics.z);
        float r=length(normal);
        float presence=step(.42,hash21(cell+float(layer)*31.0));
        float mask=(1.0-smoothstep(.7,1.0,r))*presence;
        drop.xy+=normal*mask/depth;
        // A dark rim and a tiny upper glint, multiplied into the real image below.
        drop.z+=(smoothstep(.2,.8,r)*mask*(-.45)+mask*max(0.0,normal.y)*.7)/depth;
    }
    return drop;
}
const int BAYER[16]=int[16](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
vec3 retroSurface(vec3 color) {
    vec3 result=color;
    if(Retro.z<255.5) {
        ivec2 p=ivec2(mod(floor(gl_FragCoord.xy/max(1.0,Retro.y)),4.0));
        float noise=(float(BAYER[p.y*4+p.x])+.5)/16.0-.5;
        float levels=max(3.0,Retro.z-1.0);
        result=floor(clamp(result,0.0,1.0)*levels+.5+noise*Retro.w)/levels;
    }
    if(RetroOptics.x>0.0) {
        float line=.5+.5*cos(gl_FragCoord.y*6.2831853/RetroOptics.y);
        result*=1.0-line*RetroOptics.x;
    }
    if(RetroOptics.z>0.0) {
        float stripe=mod(floor(gl_FragCoord.x),3.0);
        vec3 mask=vec3(stripe<.5?1.0:1.0-RetroOptics.z,stripe>.5&&stripe<1.5?1.0:1.0-RetroOptics.z,stripe>1.5?1.0:1.0-RetroOptics.z);
        result*=mask;
    }
    return mix(color,result,Retro.x);
}
vec3 bright(vec2 uv) {
    vec3 c=texture(SceneSampler,uv).rgb;
    float luminance=dot(c,vec3(.2126,.7152,.0722));
    return c*smoothstep(Optics.w,min(.999,Optics.w+.16),luminance);
}
void main() {
    vec2 sceneUV=texCoord;
    vec3 rain=vec3(0);
    if(Rain.x>0.0) {
        rain=lensRain(texCoord);
        sceneUV+=rain.xy*Rain.w*Rain.x/Viewport.xy;
    }
    if(Water.x>0.0) {
        float t=Balance.w*Water.z;
        vec2 ripple=vec2(sin(texCoord.y*28.0+t*1.6)+sin(texCoord.x*17.0-t*.8),cos(texCoord.x*25.0+t)+sin(texCoord.y*21.0-t*1.4))*.5;
        sceneUV+=ripple*Water.y*Water.x/Viewport.xy;
    }
    vec2 originalUV=sceneUV;
    if(Retro.x>0.0 && Retro.y>1.0) sceneUV=(floor(sceneUV*Viewport.xy/Retro.y)+.5)*Retro.y/Viewport.xy;
    vec4 source=texture(SceneSampler,sceneUV);
    if(Retro.x>0.0 && Retro.x<1.0 && Retro.y>1.0) source=mix(texture(SceneSampler,originalUV),source,Retro.x);
    vec3 color=source.rgb;
    if(Optics.x>0.0) {
        vec2 offset=(sceneUV-.5)*Optics.x/Viewport.xy;
        color.r=texture(SceneSampler,sceneUV+offset).r;
        color.b=texture(SceneSampler,sceneUV-offset).b;
    }
    float radial=length((texCoord-.5)*2.0)/1.41421356;
    if(Lens.x>0.0 && radial>.35) {
        vec2 pixel=vec2(Lens.y)/Viewport.xy;
        vec3 soft=(color+texture(SceneSampler,sceneUV+vec2(pixel.x,0)).rgb+texture(SceneSampler,sceneUV-vec2(pixel.x,0)).rgb
                  +texture(SceneSampler,sceneUV+vec2(0,pixel.y)).rgb+texture(SceneSampler,sceneUV-vec2(0,pixel.y)).rgb)*.2;
        color=mix(color,soft,smoothstep(.35,.95,radial)*Lens.x);
    }
    if(Optics.y>0.0) {
        vec2 pixel=vec2(Optics.z)/Viewport.xy;
        vec3 glow;
        if(Lens.z>.5) {
            // The small prefiltered target makes this a soft reconstruction, not separated full-res copies.
            glow=texture(GlowSampler,texCoord).rgb*.25;
            glow+=(texture(GlowSampler,texCoord+vec2(pixel.x,0)).rgb+texture(GlowSampler,texCoord-vec2(pixel.x,0)).rgb
                  +texture(GlowSampler,texCoord+vec2(0,pixel.y)).rgb+texture(GlowSampler,texCoord-vec2(0,pixel.y)).rgb)*.125;
            glow+=(texture(GlowSampler,texCoord+pixel).rgb+texture(GlowSampler,texCoord-pixel).rgb
                  +texture(GlowSampler,texCoord+vec2(pixel.x,-pixel.y)).rgb+texture(GlowSampler,texCoord+vec2(-pixel.x,pixel.y)).rgb)*.0625;
        } else glow=(bright(sceneUV+vec2(pixel.x,0))+bright(sceneUV-vec2(pixel.x,0))
                    +bright(sceneUV+vec2(0,pixel.y))+bright(sceneUV-vec2(0,pixel.y)))*.25;
        color+=glow*Optics.y;
    }
    float sceneLuma=dot(color,vec3(.2126,.7152,.0722));
    color*=exp2(Tonal.x)*(1.0+Tonal.y*smoothstep(.55,.95,sceneLuma));
    float luma=dot(color,vec3(.2126,.7152,.0722));
    color=mix(vec3(luma),color,Grade.z)*Grade.x;
    color*=vec3(1.0+Balance.x*.12+Balance.y*.06,1.0-Balance.y*.10,1.0-Balance.x*.12+Balance.y*.06);
    color=clamp(color,0.0,1.0);
    // Endpoint-preserving contrast/gamma: no additive fullbright offset.
    vec3 low=pow(color,vec3(Grade.y)), high=pow(1.0-color,vec3(Grade.y));
    color=pow(low/max(vec3(.00001),low+high),vec3(1.0/Grade.w));
    color=mix(color,color*FilterColor.rgb,FilterColor.a);
    color=mix(color,color*NightColor.rgb,NightColor.a);
    if(Effects.w>0.0) {
        vec2 cell=floor(gl_FragCoord.xy/Tonal.z)+vec2(floor(Balance.w*24.0)*13.0);
        vec3 hash=fract(vec3(cell.xyx)*.1031); hash+=dot(hash,hash.yzx+33.33);
        float grain=fract((hash.x+hash.y)*hash.z)-.5;
        float level=clamp(dot(color,vec3(.2126,.7152,.0722)),0.0,1.0);
        color*=1.0+grain*Effects.w*2.0*(.25+3.0*level*(1.0-level));
    }
    float edge=smoothstep(Effects.y,Effects.y+Effects.z,radial);
    color=mix(color,VignetteColor.rgb,edge*Effects.x*VignetteColor.a);
    color=mix(color,DamageColor.rgb,DamageColor.a);
    color=mix(source.rgb,clamp(color,0.0,1.0),Balance.z);
    if(Rain.x>0.0) color*=1.0+rain.z*RainOptics.y*Rain.x*mix(vec3(1.0),RainColor.rgb,RainColor.a);
    if(Water.x>0.0) {
        vec2 p=texCoord*vec2(Viewport.x/Viewport.y,1.0);
        float t=Balance.w*Water.z;
        float waves=sin(p.x*WaterOptics.x+t)+sin(p.y*WaterOptics.x*1.2-t*.7)+sin((p.x+p.y)*WaterOptics.x*.8+t*.4);
        float shimmer=pow(max(0.0,1.0-abs(waves)*2.0),3.0);
        vec2 siltCell=p*45.0*Silt.y+vec2(t*.18,-t*.3)*Silt.z,local=(fract(siltCell)-.5)/Silt.x;
        float shape=Silt.w<.5?length(local):Silt.w<1.5?abs(local.x)+abs(local.y):min(length(local*vec2(2.6,.8)),length(local*vec2(.8,2.6)));
        float speck=(1.0-smoothstep(.035,.08,shape))*step(.8,hash21(floor(siltCell)));
        // Screen-space decoration, multiplicative only: fog, black shadows and depth remain intact.
        color*=1.0+Water.x*(shimmer*Water.w+speck*WaterOptics.y*SiltColor.rgb*SiltColor.a);
    }
    if(Retro.x>0.0) {
        color=retroSurface(color);
    }
    fragColor=vec4(clamp(color,0.0,1.0),source.a);
}
