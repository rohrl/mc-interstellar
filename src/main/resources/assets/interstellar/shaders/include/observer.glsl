// Local camera-frame boost before the existing GR camera-to-coordinate map.
// Sight vectors point toward sources; photon propagation has the opposite sign.
uniform vec3 ObserverVelocity,ObserverEffects;
#ifdef INTERSTELLAR_NATIVE_MESH
float observerDoppler(vec3 sight) {
    float b2=dot(ObserverVelocity,ObserverVelocity);if(b2<1e-12)return 1.0;
    float gamma=inversesqrt(1.0-b2),along=dot(ObserverVelocity,sight);
    return ObserverEffects.x<.5?gamma*(1.0+along):1.0/(gamma*(1.0-along));
}
vec3 observerRay(vec3 sight) {
    float b2=dot(ObserverVelocity,ObserverVelocity);
    if(b2<1e-12||ObserverEffects.x<.5)return sight;
    float gamma=inversesqrt(1.0-b2),along=dot(ObserverVelocity,sight);
    float denominator=gamma*(1.0-along);
    return normalize((sight+ObserverVelocity*((gamma-1.0)*along/b2-gamma))/denominator);
}
// Fictional continuum through RGB anchors, with weak UV/IR extensions. These
// preserve texture brightness but cannot recover real material spectra from RGB.
float observerSpectrum(vec3 c,float wavelength) {
    float luminance=dot(c,vec3(.2126,.7152,.0722));
    float uv=.005*mix(luminance,c.b,.5),ir=.005*mix(luminance,c.r,.5);
    if(wavelength<380.0){float t=max(wavelength,0.0)/380.0;return uv*t*t*t*t;}
    if(wavelength<450.0)return mix(uv,c.b,(wavelength-380.0)/70.0);
    if(wavelength<550.0)return mix(c.b,c.g,(wavelength-450.0)/100.0);
    if(wavelength<650.0)return mix(c.g,c.r,(wavelength-550.0)/100.0);
    if(wavelength<780.0)return mix(c.r,ir,(wavelength-650.0)/130.0);
    float t=780.0/wavelength;return ir*t*t;
}
vec3 observerRadiance(vec3 c,vec2 uv) {
    if(dot(ObserverVelocity,ObserverVelocity)<1e-12)return c;
    if(ObserverEffects.y==0.0 && ObserverEffects.z==0.0)return c;
    vec2 xy=(uv*2.0-1.0)*ViewSlopes.xy;
    float doppler=observerDoppler(normalize(Forward+(xy.x+ViewSlopes.z)*Right+(-xy.y+ViewSlopes.w)*Up));
    if(abs(doppler-1.0)<1e-6)return c;
    vec3 linear=pow(max(c,vec3(0)),vec3(2.2));
    if(ObserverEffects.y>0.0) {
        // Gentle mode compresses log frequency shifts for legibility. Full shift
        // uses actual D, but colour still depends on the assumed RGB spectrum.
        float shift=pow(doppler,ObserverEffects.y);
        linear=vec3(observerSpectrum(linear,650.0*shift),observerSpectrum(linear,550.0*shift),observerSpectrum(linear,450.0*shift));
    }
    if(ObserverEffects.z>.5) {
        // D^4 bolometric brightness cue, compressed/bounded for playable exposure.
        // This is not calibrated spectral radiometry of Minecraft materials.
        float gain=exp(clamp(1.4*log(doppler),-2.3,2.3));
        linear=gain>=1.0?linear*gain/(1.0+linear*(gain-1.0)):linear*gain;
    }
    vec3 displayed=pow(max(linear,vec3(0)),vec3(1.0/2.2));
    // Display exposure floor, not extra physical emission: retain the shifted
    // hue and a 6% trace of texture brightness. Truly black inputs stay black.
    if(ObserverEffects.y>0.0) {
        float peak=max(displayed.r,max(displayed.g,displayed.b));
        float trace=.06*max(0.0,max(c.r,max(c.g,c.b)));
        displayed*=max(1.0,trace/max(peak,1e-20));
    }
    return displayed;
}
#else
// The legacy voxel laboratory has no observer potion path. Keep this large
// diagnostic program free of optional SR state (driver compiler pressure).
float observerDoppler(vec3 sight){return 1.0;}
vec3 observerRay(vec3 sight){return sight;}
vec3 observerRadiance(vec3 c,vec2 uv){return c;}
#endif
