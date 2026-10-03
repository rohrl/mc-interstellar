#version 150
uniform sampler2D Scene;
uniform vec2 InputSize,Direction;
uniform vec2 Viewport;
uniform float Seed;
out vec4 fragColor;
void main() {
    if(Seed>.5) {
        vec2 lo=floor(gl_FragCoord.xy)*InputSize/Viewport,hi=(floor(gl_FragCoord.xy)+1.0)*InputSize/Viewport;
        ivec2 base=ivec2(floor(lo)),end=ivec2(ceil(hi)),size=ivec2(InputSize);
        vec3 energy=vec3(0);
        for(int y=base.y;y<end.y;y++)for(int x=base.x;x<end.x;x++) {
            vec2 overlap=max(vec2(0),min(hi,vec2(x+1,y+1))-max(lo,vec2(x,y)));
            vec4 p=texelFetch(Scene,clamp(ivec2(x,y),ivec2(0),size-1),0);
            // Warm photographic glare is a deliberate cinematic colour response.
            energy+=pow(max(p.rgb,vec3(0)),vec3(2.2))*p.a*vec3(1.0,.72,.16)*overlap.x*overlap.y;
        }
        fragColor=vec4(energy/((hi.x-lo.x)*(hi.y-lo.y)),1);return;
    }
    vec2 uv=gl_FragCoord.xy/InputSize,stepUv=Direction/InputSize;
    // Keep the tails to four sigma: a short truncated kernel leaves visible
    // rectangular cutoff edges after strong exposure and display gamma.
    const float weight[13]=float[13](.13298454,.12579798,.10648569,.08065920,.05467158,.03315999,.01799750,.00874088,.00379877,.00147732,.00051411,.00016009,.00004461);
    vec3 colour=texture(Scene,uv).rgb*weight[0];
    for(int i=1;i<13;i++)colour+=(texture(Scene,uv+stepUv*float(i)).rgb+texture(Scene,uv-stepUv*float(i)).rgb)*weight[i];
    fragColor=vec4(colour,1);
}
