#version 330
uniform sampler2D SceneSampler;
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
};
in vec2 texCoord;
out vec4 fragColor;
vec3 bright(vec2 uv) {
    vec3 c=texture(SceneSampler,uv).rgb;
    return c*max(0.0,max(c.r,max(c.g,c.b))-Optics.w)/max(.05,1.0-Optics.w);
}
void main() {
    vec4 source=texture(SceneSampler,texCoord);
    vec3 color=source.rgb;
    if(Optics.x>0.0) {
        vec2 offset=(texCoord-.5)*Optics.x/Viewport.xy;
        color.r=texture(SceneSampler,texCoord+offset).r;
        color.b=texture(SceneSampler,texCoord-offset).b;
    }
    if(Optics.y>0.0) {
        vec2 pixel=vec2(Optics.z)/Viewport.xy;
        vec3 glow=bright(texCoord+vec2(pixel.x,0))+bright(texCoord-vec2(pixel.x,0))
                 +bright(texCoord+vec2(0,pixel.y))+bright(texCoord-vec2(0,pixel.y));
        color+=glow*(Optics.y*.25);
    }
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
        float grain=fract(sin(dot(gl_FragCoord.xy+floor(Balance.w*24.0),vec2(12.9898,78.233)))*43758.5453)-.5;
        color*=1.0+grain*Effects.w*2.0;
    }
    float edge=smoothstep(Effects.y,Effects.y+Effects.z,length((texCoord-.5)*2.0)/1.41421356);
    color=mix(color,VignetteColor.rgb,edge*Effects.x*VignetteColor.a);
    color=mix(color,DamageColor.rgb,DamageColor.a);
    fragColor=vec4(mix(source.rgb,clamp(color,0.0,1.0),Balance.z),source.a);
}
