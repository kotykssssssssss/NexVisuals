#version 330
layout(std140) uniform StarConfig {
    vec4 StarColor;
    vec4 StarParams;
    vec4 StarFlags;
};
in vec2 starUv;
in vec2 starData;
out vec4 fragColor;
void main() {
    float shape=1.0;
    if(StarFlags.x>.5 && StarFlags.x<1.5) shape=1.0-smoothstep(.75,1.0,abs(starUv.x)+abs(starUv.y));
    if(StarFlags.x>1.5) shape=pow(max(0.0,1.0-length(starUv)),1.5);
    float twinkle=1.0-StarParams.w*(.5+.5*sin(StarParams.x*StarParams.z+starData.x*6.2831853));
    float gain=StarParams.y*starData.y*twinkle;
    fragColor=vec4(StarColor.rgb*min(1.3,gain+.35),StarColor.a*clamp(gain,0.0,1.0)*shape);
}
