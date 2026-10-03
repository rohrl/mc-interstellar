// Thin, optically thick circular disk in the source's Schwarzschild areal chart.
// Inner edge is the ISCO, 3 r_s. Turbulent emissivity and display exposure are assumed.
#if defined(INTERSTELLAR_NATIVE_MESH) && !defined(INTERSTELLAR_GLOW)
float diskObserverEnergy=1.0;
void diskBegin(vec3 sight) {
    diskObserverEnergy=1.0;
    if(DiskSource.w<=0.0 || Lensing<.5)return;
    vec3 offset=Camera-DiskSource.xyz;float r=max(length(offset),1e-6);
    float mu=dot(sight,offset/r),flow=sqrt(DiskSource.w/r);
    float boost=min(.999999,flow*smoothstep(1.05,1.25,r/DiskSource.w));
    float fallMu=(mu-boost)/(1.0-boost*mu);
    // E_infinity / E_camera for the same static-to-infall camera frame as the rays.
    diskObserverEnergy=(1.0+flow*fallMu)*(1.0-boost*mu)*inversesqrt(1.0-boost*boost);
}
float diskIntersection(vec3 start,vec3 end) {
    if(DiskSource.w<=0.0)return -1.0;
    float a=dot(start-DiskSource.xyz,DiskAxis),b=dot(end-DiskSource.xyz,DiskAxis);
    if(a*b>0.0 || abs(a-b)<1e-8)return -1.0;
    float t=a/(a-b);
    if(t<=1e-6 || t>1.0)return -1.0;
    float r=length(mix(start,end,t)-DiskSource.xyz)/DiskSource.w;
    return r>3.0 && r<DiskSettings.x?t:-1.0;
}
float diskShift(vec3 point,vec3 backwards) {
    if(Lensing<.5)return 1.0;
    vec3 offset=point-DiskSource.xyz;float r=length(offset)/DiskSource.w;
    vec3 radial=normalize(offset);float f=1.0-1.0/r;
    // Convert the chord's coordinate tangent to the emitter's static orthonormal frame.
    float radialComponent=dot(backwards,radial);
    vec3 localRay=normalize(backwards+radial*(radialComponent*(inversesqrt(f)-1.0)));
    float speed=inversesqrt(2.0*(r-1.0));
    float along=dot(normalize(cross(DiskAxis,radial)),localRay);
    return sqrt(f*(1.0-speed*speed))/max(1e-6,diskObserverEnergy*(1.0+speed*along));
}
float diskHash(ivec2 cell,uint seed) {
    // Hash integer corner coordinates, not rounded float dot products. Adjacent
    // cells must get EXACTLY the same corner, even after many animation epochs.
    uint h=uint(cell.x)*1597334677u ^ uint(cell.y)*3812015801u ^ seed*2798796415u;
    h=(h^(h>>16u))*2246822519u;h=(h^(h>>13u))*3266489917u;
    return float((h^(h>>16u))>>8u)*(1.0/16777216.0);
}
// Value plus analytic gradient. Quintic interpolation avoids creases in the
// apparent relief at cell edges. No extra texture reads for a finite difference.
vec3 diskNoise(vec2 p,uint seed) {
    ivec2 cell=ivec2(floor(p));vec2 f=fract(p);
    vec2 u=f*f*f*(f*(f*6.0-15.0)+10.0),du=30.0*f*f*(f-1.0)*(f-1.0);
    float a=diskHash(cell,seed),b=diskHash(cell+ivec2(1,0),seed);
    float c=diskHash(cell+ivec2(0,1),seed),d=diskHash(cell+ivec2(1,1),seed);
    return vec3(mix(mix(a,b,u.x),mix(c,d,u.x),u.y),
                du.x*mix(b-a,d-c,u.y),du.y*mix(c-a,d-b,u.x));
}
float diskPattern(float r,float angle,float age,uint seed,float footprint,vec2 viewSlope) {
    float omega=inversesqrt(2.0*r*r*r),phase=angle-omega*age;
    vec2 radial=vec2(cos(phase),sin(phase));
    float a=angle-omega*age*1.04,b=angle-omega*age*.94;
    vec3 broad=diskNoise(vec2(cos(a),sin(a))*r*.85,seed);
    vec3 eddy=diskNoise(vec2(cos(b),sin(b))*r*2.3,seed+17u);
    // Two emitting layers with a very small apparent separation. Parallax is
    // bounded at grazing views: this suggests depth without moving the annulus,
    // changing silhouette/occlusion, or pretending to simulate a gas volume.
    float rotate=-omega*age;
    mat2 turn=mat2(cos(rotate),sin(rotate),-sin(rotate),cos(rotate));
    vec2 q=radial*r+turn*viewSlope*(broad.x-.5)*.025;
    q+=vec2(eddy.x-.5,broad.x-.5)*.16;
    vec3 grain=diskNoise(q*7.0,seed+43u),micro=diskNoise(q*19.0,seed+101u);
    float w1=1.0-smoothstep(.35,.85,footprint*r*2.3);
    float w2=1.0-smoothstep(.35,.85,footprint*r*7.0);
    float w3=1.0-smoothstep(.35,.85,footprint*r*19.0);
    float density=.54*(broad.x-.5)+.34*w1*(eddy.x-.5)
        +.23*w2*(grain.x-.5)+.12*w3*(micro.x-.5);
    float bands=sin(34.0*log(r/3.0)+7.0*broad.x+4.0*eddy.x+2.0*phase);
    float bandWeight=1.0-smoothstep(.012,.045,footprint);
    // Inner-facing hot rims and cooler pockets suggest turbulent relief. This
    // is an emissivity model, not Lambertian illumination of a solid surface.
    float relief=clamp(-dot(grain.yz,radial)*.13*w2,-.18,.18);
    float filaments=exp2(3.5*density+.22*bands*bandWeight+relief)-.16;
    float seconds=DiskSettings.z*DiskSource.w/60.0;
    float shimmer=1.0+.04*sin(seconds*2.7+broad.x*11.0+float(seed%31u))
        +.02*sin(seconds*5.1+eddy.x*17.0+float(seed%47u));
    return clamp(filaments*shimmer,.08,2.3);
}
vec3 diskRadiance(vec3 point,vec3 backwards) {
    vec3 offset=(point-DiskSource.xyz)/DiskSource.w;float r=length(offset);
    vec3 axisX=normalize(cross(DiskAxis,vec3(0,0,1))),axisY=cross(DiskAxis,axisX);
    float angle=atan(dot(offset,axisY),dot(offset,axisX));
    vec2 viewSlope=vec2(dot(backwards,axisX),dot(backwards,axisY))
        /max(.2,abs(dot(backwards,DiskAxis)));
    float time=DiskSettings.z,epoch=floor(time/24.0),age=mod(time,24.0);
    float pixelAngle=2.0*max(ViewSlopes.x/Viewport.x,ViewSlopes.y/Viewport.y);
    float footprint=pixelAngle*length(point-Camera)/max(DiskSource.w*r,1e-5)
        /max(.035,abs(dot(backwards,DiskAxis)));
    uint generation=uint(int(epoch));
    float pattern=mix(diskPattern(r,angle,age+24.0,generation-1u,footprint,viewSlope),
                      diskPattern(r,angle,age,generation,footprint,viewSlope),smoothstep(0.0,24.0,age));
    // Zero-torque Newtonian thin-disk temperature profile, used as a declared
    // emissivity approximation on relativistic circular orbits (not GRMHD).
    float profile=(1.0-sqrt(3.0/r))/(r*r*r);
    float peakRadius=49.0/12.0;
    float peakProfile=(1.0-sqrt(3.0/peakRadius))/(peakRadius*peakRadius*peakRadius);
    float temperature=DiskSettings.w*pow(max(profile/peakProfile,1e-8),.25);
    temperature*=clamp(diskShift(point,backwards),.02,50.0);
    // Planck samples in RGB bands, white-balanced at 6500 K. B_lambda(g*T)
    // already carries the spectral Doppler amplification; do not apply g^4 again.
    vec3 wavelength=vec3(650,550,450),c2=vec3(14387769.0)/wavelength;
    vec3 linear=(exp(c2/6500.0)-1.0)/(exp(min(c2/max(temperature,1.0),vec3(80)))-1.0);
    linear*=DiskSettings.y*pattern;
    return pow(linear/(vec3(1)+linear),vec3(1.0/2.2));
}
#else
void diskBegin(vec3 sight){}
float diskIntersection(vec3 start,vec3 end){return -1.0;}
float diskShift(vec3 point,vec3 backwards){return 1.0;}
vec3 diskRadiance(vec3 point,vec3 backwards){return vec3(0);}
#endif
