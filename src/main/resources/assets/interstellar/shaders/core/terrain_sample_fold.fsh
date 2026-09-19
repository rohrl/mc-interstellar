#version 150
uniform sampler2D Samples;
uniform vec2 Viewport;
out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    fragColor=(texelFetch(Samples,pixel,0)+texelFetch(Samples,pixel+ivec2(int(Viewport.x),0),0))*.5;
}
