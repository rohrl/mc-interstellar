#version 150
uniform sampler2D Scene;
uniform vec2 Texel;
in vec2 screenUv;
out vec4 fragColor;
void main() {
    vec4 centre=texture(Scene,screenUv),outside=vec4(0);
    for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++) {
        vec4 value=texture(Scene,clamp(screenUv+vec2(x,y)*Texel,Texel*.5,vec2(1)-Texel*.5));
        if(value.a>outside.a)outside=value;
    }
    // Mask RGB is premultiplied by the two-ray coverage.
    fragColor=vec4(outside.rgb/max(outside.a,.0001),max(0,outside.a-centre.a));
}
