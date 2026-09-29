#version 150
uniform sampler2D Previous;
uniform float Opacity;
in vec2 screenUv;
out vec4 fragColor;
void main() {fragColor=vec4(texture(Previous,vec2(screenUv.x,1-screenUv.y)).rgb,Opacity);}
