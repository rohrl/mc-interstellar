// Two holes in ONE exterior. The Fermat gradient is Ellis near each throat,
// smoothly zero outside disjoint influence spheres. See docs/science.md.
float localWormholeGradient(float r) {
    float k=Radius*Radius*.25,inner=(Radius*.5+WormholeInfluence)*.5;
    return -2.0*k/(r*(r*r+k))*(1.0-smoothstep(inner,WormholeInfluence,r));
}
// State: coordinate radius, cosine of ray/radial angle, orbital angle.
// Parameter: Euclidean arc length. No inverse-trig or index integral required.
vec3 localWormholeDerivative(vec3 q) {
    float r=max(q.x,1e-5),s2=max(0.0,1.0-q.y*q.y);
    return vec3(q.y,s2*(1.0/r+localWormholeGradient(r)),sqrt(s2)/r);
}
vec3 localWormholeStep(vec3 q,float h) {
    vec3 a=localWormholeDerivative(q),b=localWormholeDerivative(q+h*a*.5);
    vec3 c=localWormholeDerivative(q+h*b*.5),d=localWormholeDerivative(q+h*c);
    vec3 next=q+h*(a+2.0*b+2.0*c+d)/6.0;next.y=clamp(next.y,-1.0,1.0);return next;
}
vec3 localMouth(int end) {return end==0?Source:OtherSource;}
float localMouthEntry(vec3 p,vec3 d,vec3 centre) {
    vec3 offset=p-centre;float r=length(offset),along=dot(offset,d);
    if(r<WormholeInfluence-.001)return 0.0;
    if(along>=0.0)return 1e20;
    float discriminant=along*along-dot(offset,offset)+WormholeInfluence*WormholeInfluence;
    return discriminant>0.0?max(0.0,-along-sqrt(discriminant)):1e20;
}
void localRayFrame(vec3 p,vec3 d,int end,out vec3 radial,out vec3 tangent,out vec3 q) {
    vec3 offset=p-localMouth(end);float r=max(length(offset),1e-5);
    radial=offset/r;float mu=clamp(dot(d,radial),-1.0,1.0);
    vec3 t=d-mu*radial;float sine=length(t);
    tangent=sine>1e-7?t/sine:normalize(cross(radial,abs(radial.y)<.9?vec3(0,1,0):vec3(1,0,0)));
    q=vec3(r,mu,0);
}
bool localSceneChord(vec3 start,vec3 end,inout int layers) {
    wormholeChordStart=start;vec3 delta=end-start,hit,normal,p=start;float distance=length(delta);
    if(distance>1e-7 && Diagnostic<3.5)for(int layer=0;layer<32;layer++) {
        int value=sceneSegment(p,end,hit,normal);
#ifdef INTERSTELLAR_MATERIALS
        if(passMaterial(value,hit,normal)) {
            p=pastSurface(hit,normal,delta);
            if(++layers>=int(MaterialLimit)){fragColor=vec4(0,0,0,1);return true;}
            if(dot(end-p,delta)>0.0)continue;
            break;
        }
#endif
        if(value>0) {fragColor=vec4(surface(value,hit,normal),1);return true;}
        break;
    }
    wormholeTravelled+=sceneFogLength(delta);return false;
}
void traceLocalWormholes(vec2 uv) {
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
    vec3 direction=observerRay(normalize(Forward+(xy.x+ViewSlopes.z)*Right+(-xy.y+ViewSlopes.w)*Up)),p=Camera;
    int layers=0,passages=0,mouthIndex=-1;float mouth=Radius*.5,angle=0.0;
    if(Lensing<.5) {
        if(!localSceneChord(p,p+direction*WormholeExtent,layers))fragColor=vec4(missing(direction),1);return;
    }
    // Preserve the same physical view for a camera still inside a throat while
    // awaiting its server chart change (or the explicit step-out entry guard).
    for(int end=0;end<2;end++) {
        vec3 offset=p-localMouth(end);float rr=dot(offset,offset);
        if(rr<mouth*mouth) {
            if(rr<1e-10){offset=-direction*.0001;rr=dot(offset,offset);}
            p=localMouth(1-end)+wormholeReflect(offset)*(mouth*mouth/rr);
            direction=normalize(wormholeReflect(direction-2.0*offset*dot(offset,direction)/rr));
            passages=1;break;
        }
    }
    vec3 radial=vec3(0),tangent=vec3(0),q=vec3(0);
    for(int iteration=0;iteration<2048;iteration++) {
        if(mouthIndex<0) {
            float a=localMouthEntry(p,direction,Source),b=localMouthEntry(p,direction,OtherSource);
            int nextEnd=a<=b?0:1;float distance=min(a,b);
            vec3 end=p+direction*min(distance,WormholeExtent);
            if(localSceneChord(p,end,layers))return;
            if(distance>=WormholeExtent) {
                diagnostic=vec4(direction,float(passages));fragColor=vec4(missing(direction),1);return;
            }
            mouthIndex=nextEnd;p=end;localRayFrame(p,direction,mouthIndex,radial,tangent,q);
        }
        float sine=sqrt(max(0.0,1.0-q.y*q.y));
        float curvature=abs(localWormholeGradient(q.x))*sine;
        float tolerance=.001*(PathStep/.45)*(PathStep/.45)*CurveFactor;
        float h=clamp(sqrt(8.0*tolerance/max(curvature,1e-12)),.02,MeshStepLimit);
        h=min(h,min(.04,OrbitStep)*q.x/max(sine,.0001));
        h=min(h,.12*q.x/max(abs(q.y),.01));
        vec3 next=localWormholeStep(q,h);
        bool crossing=next.x<mouth,leaving=next.x>=WormholeInfluence && next.y>0.0;
        if(crossing || leaving) {
            float boundary=crossing?mouth:WormholeInfluence;
            // At a grazing entry r already equals the outer boundary. Starting
            // Newton at zero would select that entry again, never the later exit.
            float stepSize=leaving?h:clamp((boundary-q.x)/q.y,0.0,h);
            for(int root=0;root<4;root++) {
                next=localWormholeStep(q,stepSize);
                if(abs(next.y)>.000001)stepSize=clamp(stepSize-(next.x-boundary)/next.y,0.0,h);
            }
            next=localWormholeStep(q,stepSize);next.x=boundary;
        }
        vec3 n=cos(next.z)*radial+sin(next.z)*tangent,t=-sin(next.z)*radial+cos(next.z)*tangent;
        vec3 end=localMouth(mouthIndex)+next.x*n;
        if(localSceneChord(p,end,layers))return;
        angle+=next.z-q.z;returningBody=angle>3.14159265 || passages>0;
        p=end;q=next;
        direction=normalize(q.y*n+sqrt(max(0.0,1.0-q.y*q.y))*t);
        if(crossing) {
            if(passages>=4) {diagnostic=vec4(0,0,0,-3);fragColor=vec4(0,0,0,1);return;}
            passages++;mouthIndex=1-mouthIndex;
            p=localMouth(mouthIndex)+mouth*wormholeReflect(n);
            direction=normalize(wormholeReflect(direction-2.0*n*dot(n,direction)));
            localRayFrame(p,direction,mouthIndex,radial,tangent,q);q.x=mouth;
            for(int tree=0;tree<SCENE_TREES;tree++)cellKnown[tree]=false;
        } else if(leaving)mouthIndex=-1;
        if(angle>25.1327412) {diagnostic=vec4(0,0,0,-3);fragColor=vec4(0,0,0,1);return;}
    }
    diagnostic=vec4(0,0,0,-2);fragColor=vec4(.7,.05,.5,1);
}
