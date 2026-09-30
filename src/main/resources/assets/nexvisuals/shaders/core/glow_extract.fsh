#version 330
uniform sampler2D SceneSampler;
layout(std140) uniform HighlightConfig { vec4 Highlight; };
in vec2 texCoord;
out vec4 fragColor;
vec3 extract(vec2 uv) {
    vec3 color=texture(SceneSampler,uv).rgb;
    float luminance=dot(color,vec3(.2126,.7152,.0722));
    return color*smoothstep(Highlight.z,min(.999,Highlight.z+.16),luminance);
}
void main() {
    // Four linear-filtered samples prefilter a 4x4 source footprint into each quarter-resolution pixel.
    vec2 offset=1.0/Highlight.xy;
    vec3 bright=(extract(texCoord+offset)+extract(texCoord-offset)
                +extract(texCoord+vec2(offset.x,-offset.y))+extract(texCoord+vec2(-offset.x,offset.y)))*.25;
    fragColor=vec4(bright,1.0);
}
