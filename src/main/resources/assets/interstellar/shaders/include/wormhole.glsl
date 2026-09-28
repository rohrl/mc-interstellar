// Ultrastatic Ellis metric, ds²=-dt²+dl²+(l²+a²)dOmega².
// State is (l/a, dl/ds, phi); h and impact are in units of the areal throat a.
// Same Hamiltonian equations as the independent CPU/elliptic-integral reference.
vec3 wormholeDerivative(vec3 q,float impact) {
    float rr=1.0+q.x*q.x;
    return vec3(q.y,impact*impact*q.x/(rr*rr),impact/rr);
}
vec3 wormholeStep(vec3 q,float impact,float h) {
    vec3 a=wormholeDerivative(q,impact),b=wormholeDerivative(q+h*a*.5,impact);
    vec3 c=wormholeDerivative(q+h*b*.5,impact),d=wormholeDerivative(q+h*c,impact);
    return q+h*(a+2.0*b+2.0*c+d)/6.0;
}
vec3 wormholeReflect(vec3 p) {return p*vec3(1,1,-1);}
float wormholeRadius(float ell) {return Radius*.5*(sqrt(1.0+ell*ell)+abs(ell));}
vec3 wormholePosition(vec3 q,vec3 radial,vec3 tangent,bool other) {
    vec3 n=cos(q.z)*radial+sin(q.z)*tangent;
    return (other?OtherSource:Source)+wormholeRadius(q.x)*(other?wormholeReflect(n):n);
}
vec3 wormholeDirection(vec3 q,float impact,vec3 radial,vec3 tangent,bool other) {
    vec3 n=cos(q.z)*radial+sin(q.z)*tangent,t=-sin(q.z)*radial+cos(q.z)*tangent;
    vec3 d=(other?-q.y:q.y)*n+impact/sqrt(1.0+q.x*q.x)*t;
    return normalize(other?wormholeReflect(d):d);
}
void trace(vec2 uv) {
#ifdef INTERSTELLAR_GLOW
    glowHit=false;
#endif
    returningBody=false;wormholeTravelled=0.0;
    cloudLayer=vec4(0);diagnostic=vec4(0);distantHit=false;
    for(int tree=0;tree<SCENE_TREES;tree++)cellKnown[tree]=false;
#ifdef INTERSTELLAR_MATERIALS
    materialLayers=vec4(0);cloudSeen=false;
#endif
    vec2 xy=(uv*2.0-1.0)*ViewSlopes.xy;
    vec3 direction=normalize(Forward+(xy.x+ViewSlopes.z)*Right+(-xy.y+ViewSlopes.w)*Up);
    vec3 offset=Camera-Source;float r=max(length(offset),1e-5),a=Radius;
    vec3 radial=offset/r;
    float mu=clamp(dot(direction,radial),-1.0,1.0);
    vec3 t=direction-mu*radial;float sine=length(t);
    vec3 tangent=sine>1e-7?t/sine:normalize(cross(radial,abs(radial.y)<.9?vec3(0,1,0):vec3(1,0,0)));
    float ell=(r-a*a/(4.0*r))/a;
    float impact=sqrt(1.0+ell*ell)*sine;
    vec3 q=vec3(ell,mu,0);
    wormholeOther=ell<0.0 || ell==0.0 && mu<0.0;
    vec3 p=wormholePosition(q,radial,tangent,wormholeOther),hit,normal;
    if(Lensing<.5) {
        wormholeOther=false;wormholeChordStart=Camera;
        int value=sceneSegment(Camera,Camera+direction*WormholeExtent,hit,normal);
        fragColor=vec4(value>0?surface(value,hit,normal):missing(direction),1);return;
    }
    int layers=0;
    for(int iteration=0;iteration<2048;iteration++) {
        float rr=1.0+q.x*q.x,R=wormholeRadius(q.x),k=a*a*.25;
        // Spatial curvature of rays in the isotropic Fermat index 1+k/R².
        // Chord sagitta ~ curvature*length²/8. Also bound RK/angular increments.
        float curvature=2.0*k*(impact/sqrt(rr))/(R*(R*R+k));
        float tolerance=.001*(PathStep/.45)*(PathStep/.45)*CurveFactor;
        float spatial=clamp(sqrt(8.0*tolerance/max(curvature,1e-12)),.02,MeshStepLimit);
        float h=min(spatial*(1.0+k/(R*R))/a,min(.04,OrbitStep)*rr/max(impact,1e-7));
        h=min(h,.12*max(1.0,abs(q.x))/max(abs(q.y),.01));
        // Outward rays beyond all captured geometry still need their remaining
        // weak bending integrated for the sky; larger steps there cost no queries.
        bool beyond=R>WormholeExtent && q.x*q.y>0.0;
        if(beyond)h=min(.08*abs(q.x)/max(abs(q.y),.01),.02*rr/max(impact,1e-7));
        vec3 next=wormholeStep(q,impact,h);
        bool crossing=q.x!=0.0 && q.x*next.x<0.0;
        if(crossing) {
            // Solve l(h)=0 before mapping to the other end. Never query one chord
            // connecting the distant mouth centres. RK slope gives fast Newton steps.
            float boundary=clamp(-q.x/q.y,0.0,h);
            for(int root=0;root<4;root++) {
                next=wormholeStep(q,impact,boundary);
                boundary=clamp(boundary-next.x/next.y,0.0,h);
            }
            h=boundary;next=wormholeStep(q,impact,h);next.x=0.0;
        }
        vec3 end=wormholePosition(next,radial,tangent,wormholeOther);
        if(!beyond && Diagnostic<3.5) {
            wormholeChordStart=p;
            vec3 start=p,delta=end-p;
            float lengthSquared=dot(delta,delta);
            if(lengthSquared>1e-14)for(int layer=0;layer<32;layer++) {
                int value=sceneSegment(start,end,hit,normal);
#ifdef INTERSTELLAR_MATERIALS
                if(passMaterial(value,hit,normal)) {
                    start=pastSurface(hit,normal,delta);
                    if(++layers>=int(MaterialLimit)){fragColor=vec4(0,0,0,1);return;}
                    if(dot(end-start,delta)>0.0)continue;
                    break;
                }
#endif
                if(value>0) {fragColor=vec4(surface(value,hit,normal),1);return;}
                break;
            }
            wormholeTravelled+=sqrt(lengthSquared);
        }
        q=next;p=end;returningBody=q.z>3.14159265;
        if(crossing) {
            wormholeOther=!wormholeOther;p=wormholePosition(q,radial,tangent,wormholeOther);
            for(int tree=0;tree<SCENE_TREES;tree++)cellKnown[tree]=false;
        } else if(q.x==0.0 && q.y!=0.0) {
            // A camera can start precisely on the throat.
            bool side=q.y<0.0;
            if(side!=wormholeOther) {wormholeOther=side;p=wormholePosition(q,radial,tangent,side);}
        }
        if(abs(q.x)>2048.0 && q.x*q.y>0.0) {
            // Residual angle is impact/|l| + O(l^-3). The correction cancels the
            // finite-distance tangent angle; direction below uses the radial limit.
            float tail=impact/abs(q.x);
            vec3 n=cos(q.z+tail)*radial+sin(q.z+tail)*tangent;
            diagnostic=vec4(wormholeOther?wormholeReflect(n):n,wormholeOther?1.0:0.0);
            fragColor=vec4(missing(wormholeOther?wormholeReflect(n):n),1);return;
        }
        if(q.z>25.1327412) {fragColor=vec4(dark(),1);diagnostic=vec4(0,0,0,2);return;}
    }
    diagnostic=vec4(0,0,0,-2);fragColor=vec4(.7,.05,.5,1);
}
