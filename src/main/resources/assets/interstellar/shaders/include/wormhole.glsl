// Gameplay uses two localized mouths in one continuous exterior.
// The exact isolated Ellis reference remains in science/EllisWormhole.java.
vec3 wormholeReflect(vec3 p) {return p*vec3(1,1,-1);}
#moj_import <interstellar:wormhole_local.glsl>
void trace(vec2 uv) {traceLocalWormholes(uv);}
