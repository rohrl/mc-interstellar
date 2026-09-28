#version 150
uniform sampler2D Samples;
uniform vec2 Viewport;
uniform float SampleCount;
out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    vec4 first=texelFetch(Samples,pixel,0);
    if(SampleCount<1.5){fragColor=first;return;}
    vec4 sum=first+texelFetch(Samples,pixel+ivec2(int(Viewport.x),0),0);
    for(int i=2;i<8;i++) {
        if(i>=int(SampleCount))break;
        sum+=texelFetch(Samples,pixel+ivec2(i%2,i/2)*ivec2(Viewport),0);
    }
    fragColor=sum/SampleCount;
}
