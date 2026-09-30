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
};
in vec2 texCoord;
out vec4 fragColor;
vec3 bright(vec2 uv) {
    vec3 c=texture(SceneSampler,uv).rgb;
    float luminance=dot(c,vec3(.2126,.7152,.0722));
    return c*smoothstep(Optics.w,min(.999,Optics.w+.16),luminance);
}
void main() {
    vec4 source=texture(SceneSampler,texCoord);
    vec3 color=source.rgb;
    if(Optics.x>0.0) {
        vec2 offset=(texCoord-.5)*Optics.x/Viewport.xy;
        color.r=texture(SceneSampler,texCoord+offset).r;
        color.b=texture(SceneSampler,texCoord-offset).b;
    }
    float radial=length((texCoord-.5)*2.0)/1.41421356;
    if(Lens.x>0.0 && radial>.35) {
        vec2 pixel=vec2(Lens.y)/Viewport.xy;
        vec3 soft=(color+texture(SceneSampler,texCoord+vec2(pixel.x,0)).rgb+texture(SceneSampler,texCoord-vec2(pixel.x,0)).rgb
                  +texture(SceneSampler,texCoord+vec2(0,pixel.y)).rgb+texture(SceneSampler,texCoord-vec2(0,pixel.y)).rgb)*.2;
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
        } else glow=(bright(texCoord+vec2(pixel.x,0))+bright(texCoord-vec2(pixel.x,0))
                    +bright(texCoord+vec2(0,pixel.y))+bright(texCoord-vec2(0,pixel.y)))*.25;
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
    fragColor=vec4(mix(source.rgb,clamp(color,0.0,1.0),Balance.z),source.a);
}
