#version 330
#moj_import <minecraft:fog.glsl>
layout(std140) uniform SkyConfig {
    vec4 SkyColor;
    vec4 HorizonColor;
    vec4 ZenithColor;
    vec4 Gradient;
    vec4 Atmosphere;
    vec4 NebulaColor;
    vec4 AuroraColor;
    vec4 Details;
    vec4 GlowColor;
};
in vec3 skyDirection;
out vec4 fragColor;
float hash3(vec3 p) {
    // Arithmetic hash avoids sixteen trigonometric hash evaluations per nebula pixel.
    p=fract(p*.1031); p+=dot(p,p.yzx+33.33);
    return fract((p.x+p.y)*p.z);
}
float skyNoise(vec3 p) {
    vec3 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(mix(hash3(i),hash3(i+vec3(1,0,0)),f.x),mix(hash3(i+vec3(0,1,0)),hash3(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash3(i+vec3(0,0,1)),hash3(i+vec3(1,0,1)),f.x),mix(hash3(i+vec3(0,1,1)),hash3(i+vec3(1,1,1)),f.x),f.y),f.z);
}
void main() {
    vec3 d=normalize(skyDirection);
    float height=d.y-Gradient.y, softness=Gradient.z;
    vec3 gradient=mix(HorizonColor.rgb,SkyColor.rgb,smoothstep(-softness*.2,softness,height));
    gradient=mix(gradient,ZenithColor.rgb,smoothstep(.2,1.0,height));
    vec3 color=mix(SkyColor.rgb,gradient,Gradient.x);
    float night=Atmosphere.x, rain=Atmosphere.y, time=Gradient.w*Details.z;
    float visible=smoothstep(-.02,.14,d.y)*night*rain;
    if(Atmosphere.z>0.0) {
        vec3 p=d*4.5+vec3(time*.006,0.0,time*.004);
        float cloud=skyNoise(p)*.65+skyNoise(p*2.7)*.35;
        float band=exp(-pow((d.y-.48-sin(d.x*3.0+d.z*2.0)*.16)*3.0,2.0));
        color += NebulaColor.rgb*NebulaColor.a*smoothstep(.38,.85,cloud)*band*Atmosphere.z*visible;
    }
    if(Atmosphere.w>0.0) {
        float wave=sin(d.x*9.0+time*.06)+sin(d.z*6.0-time*.04)*.25;
        float ribbon=exp(-abs(d.y-(.32+wave*.09))*24.0);
        float curtain=.45+.55*pow(.5+.5*sin(d.x*55.0+d.z*41.0+time*.08),3.0);
        color += AuroraColor.rgb*AuroraColor.a*ribbon*curtain*Atmosphere.w*visible;
    }
    color += GlowColor.rgb*GlowColor.a*exp(-pow(height/max(.03,softness*.35),2.0))*Details.x*(.15+.85*Details.w)*rain;
    if(Details.y>0.0 && visible>0.001) {
        // One bounded meteor per interval; no particle list or world-position history.
        float age=mod(Gradient.w,Details.y), cycle=floor(Gradient.w/Details.y);
        if(age<1.3) {
            float start=hash3(vec3(cycle,4,2))*6.2831853-3.14159265;
            float angle=atan(d.z,d.x)-start-age*.42;
            angle=atan(sin(angle),cos(angle));
            vec2 p=vec2(angle,asin(clamp(d.y,-1.0,1.0))-(.72-age*.20));
            vec2 tail=vec2(-.18,.086);
            float t=clamp(dot(p,tail)/dot(tail,tail),0.0,1.0);
            float line=exp(-length(p-tail*t)*330.0)*(1.0-t);
            color += vec3(.7,.82,1.0)*line*sin(age/1.3*3.14159265)*visible;
        }
    }
    color=mix(color,FogColor.rgb,(1.0-rain)*.5);
    color=mix(FogColor.rgb,color,smoothstep(-.15,.03,d.y));
    // Match the vanilla 16-unit sky disc's angular fog distance; fluid/effect sky remains vanilla.
    float distance=16.0/max(.04,abs(d.y));
    fragColor=apply_fog(vec4(clamp(color,0.0,1.0),1.0),distance,distance,0.0,FogSkyEnd,FogSkyEnd,FogSkyEnd,FogColor);
}
