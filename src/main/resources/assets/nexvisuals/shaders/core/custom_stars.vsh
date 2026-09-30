#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 starUv;
out vec2 starData;
void main() {
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    gl_Position.z=gl_Position.w*.99998;
    starUv=UV0*2.0-1.0;
    starData=Color.rg;
}
