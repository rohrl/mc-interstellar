#version 150
uniform sampler2D Scene;
uniform vec2 Texel;
uniform float EdgeAA;
uniform sampler2D Bloom;
uniform float BloomStrength;
uniform float GasOpacity;
in vec2 screenUv;
out vec4 fragColor;
float luma(vec3 c) {return dot(c,vec3(.2126,.7152,.0722));}
vec3 sampleScene(vec2 uv) {return texture(Scene,clamp(uv,Texel*.5,vec2(1)-Texel*.5)).rgb;}
vec4 display(vec3 colour,vec2 uv) {
    if(BloomStrength>0.0) {
        vec3 energy=texture(Bloom,uv).rgb*BloomStrength;
        colour=pow(vec3(1)-(vec3(1)-pow(clamp(colour,0.0,1.0),vec3(2.2)))*exp(-energy),vec3(1.0/2.2));
    }
    // Local opt-in cinematic gas veil; stays beneath hands and the HUD.
    return vec4(mix(colour,vec3(.55,.28,.08),GasOpacity),1);
}
vec4 cubic(float t) {
    return vec4(-.5*t+t*t-.5*t*t*t,1-2.5*t*t+1.5*t*t*t,
                .5*t+2*t*t-1.5*t*t*t,-.5*t*t+.5*t*t*t);
}
vec3 sharpScene(vec2 uv) {
    vec2 pixel=uv/Texel-.5,f=fract(pixel);ivec2 base=ivec2(floor(pixel)),size=textureSize(Scene,0);
    vec4 wx=cubic(f.x),wy=cubic(f.y);vec3 colour=vec3(0),lo=vec3(1),hi=vec3(0);
    for(int y=0;y<4;y++)for(int x=0;x<4;x++) {
        vec3 c=texelFetch(Scene,clamp(base+ivec2(x-1,y-1),ivec2(0),size-1),0).rgb;
        colour+=c*wx[x]*wy[y];
        if(x>=1 && x<=2 && y>=1 && y<=2){lo=min(lo,c);hi=max(hi,c);}
    }
    // Bounded cubic reconstruction preserves contrast without new bright/dark edge overshoot.
    return clamp(colour,lo,hi);
}
void main() {
    vec2 uv=vec2(screenUv.x,1-screenUv.y);
    vec3 centre=sampleScene(uv);
    if(EdgeAA<0.0){fragColor=display(centre,uv);return;}
    if(EdgeAA<.5) {fragColor=display(sharpScene(uv),uv);return;}
    float n=luma(sampleScene(uv+vec2(0,Texel.y))),s=luma(sampleScene(uv-vec2(0,Texel.y)));
    float e=luma(sampleScene(uv+vec2(Texel.x,0))),w=luma(sampleScene(uv-vec2(Texel.x,0)));
    float lo=min(luma(centre),min(min(n,s),min(e,w))),hi=max(luma(centre),max(max(n,s),max(e,w)));
    if(hi-lo<max(.025,hi*.10)) {fragColor=display(centre,uv);return;}
    // Smooth along the local edge, with a short bounded kernel. No history or extra optical rays.
    vec2 tangent=vec2(-(n-s),e-w);
    tangent/=max(length(tangent),.0001);
    vec2 offset=tangent*Texel*.75;
    vec3 neighbours=(sampleScene(uv-offset)+sampleScene(uv+offset))*.5;
    fragColor=display(mix(centre,neighbours,.5),uv);
}
