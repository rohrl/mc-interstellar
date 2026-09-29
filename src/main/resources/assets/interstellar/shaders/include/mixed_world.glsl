// Shared-world composition. Add spatial ray curvatures, not full-frame images.
// Exact single-centre spatial equations in each strong-field region; overlapping
// fields use an explicit gameplay approximation, not a multi-source GR solution.
float mixedMassExtent() {return max(96.0,max(48.0*MassRadius,8.0*MassBodyRadius));}
float mixedWindow(float r,float extent) {return 1.0-smoothstep(extent*.5,extent,r);}
float mixedMassCoefficient(float r,float mu,float rs,float body) {
    float s2=max(0.0,1.0-mu*mu);
    if(body<=0.0 || r>=body)return -1.5*rs*s2/max(r*r,1e-12);
    // Rewrite the existing interior orbit first integral in Euclidean arc length.
    // Unlike inverse radius this form is regular at the centre of a finite body.
    float c=rs/body,t=rs*r*r/(body*body*body),b=1.0-t;
    return -rs*r*(mu*mu/b+s2)*(3.0-c-2.0*t)/
        (2.0*body*body*body*(1.0-.5*c-.5*t));
}
vec3 mixedAcceleration(vec3 p,vec3 d) {
    vec3 acceleration=vec3(0),offset=p-MassSource;float r=length(offset);
    if(MassRadius>0.0 && r>1e-7 && r<mixedMassExtent()) {
        vec3 n=offset/r;float mu=dot(n,d);
        acceleration+=mixedMassCoefficient(r,mu,MassRadius,MassBodyRadius)*mixedWindow(r,mixedMassExtent())*(n-mu*d);
    }
    for(int i=0;i<2;i++) {
        if(float(i)>=PortalCount)break;
        offset=p-localMouth(i);r=length(offset);if(r<1e-7 || r>=WormholeInfluence)continue;
        vec3 n=offset/r;float mu=dot(n,d);
        float coefficient=PortalOpen>.5?localWormholeGradient(r):mixedMassCoefficient(r,mu,ClosedRadius,0.0)*mixedWindow(r,WormholeInfluence);
        acceleration+=coefficient*(n-mu*d);
    }
    return acceleration;
}
void mixedStep(vec3 p,vec3 d,float h,out vec3 nextP,out vec3 nextD) {
    vec3 a=mixedAcceleration(p,d),db=normalize(d+.5*h*a);
    vec3 b=mixedAcceleration(p+.5*h*d,db),dc=normalize(d+.5*h*b);
    vec3 c=mixedAcceleration(p+.5*h*db,dc),dd=normalize(d+h*c);
    vec3 e=mixedAcceleration(p+h*dc,dd);
    nextP=p+h*(d+2.0*db+2.0*dc+dd)/6.0;
    nextD=normalize(d+h*(a+2.0*b+2.0*c+e)/6.0);
}
float mixedEntry(vec3 p,vec3 d,vec3 centre,float radius) {
    vec3 v=p-centre;float b=dot(v,d),c=dot(v,v)-radius*radius;
    if(b>=0.0)return 1e20;if(c<-.001)return 0.0;
    float disc=b*b-c;return disc>0.0?max(0.0,-b-sqrt(disc)):1e20;
}
float mixedContact(vec3 p,vec3 end,vec3 centre,float radius) {
    vec3 delta=end-p;float distance=length(delta);
    if(distance<1e-8)return 2.0;
    return mixedEntry(p,delta/distance,centre,radius)/distance;
}
float mixedRadialStep(vec3 offset,vec3 d) {
    float r=length(offset),mu=dot(offset,d)/max(r,1e-8),sine=sqrt(max(0.0,1.0-mu*mu));
    // Bound angular and radial motion separately, as in the single-mouth solver.
    // A nearly radial ray needs fewer steps without loosening chord-error limits.
    return min(min(.04,OrbitStep)*r/max(sine,.0001),.12*r/max(abs(mu),.01));
}
void traceMixedWorld(vec2 uv) {
#ifdef INTERSTELLAR_GLOW
    glowHit=false;
#endif
    mixedEditor=false;mixedInterior=false;returningBody=false;wormholeTravelled=0.0;
    cloudLayer=vec4(0);diagnostic=vec4(0);distantHit=false;
    for(int tree=0;tree<SCENE_TREES;tree++)cellKnown[tree]=false;
#ifdef INTERSTELLAR_MATERIALS
    materialLayers=vec4(0);cloudSeen=false;
#endif
    vec2 xy=(uv*2.0-1.0)*ViewSlopes.xy;
    vec3 d=observerRay(normalize(Forward+(xy.x+ViewSlopes.z)*Right+(-xy.y+ViewSlopes.w)*Up)),p=Camera;
    int layers=0,passages=0;float turn=0.0,mouth=Radius*.5;bool skyAllowed=true;
    if(Lensing<.5) {if(!localSceneChord(p,p+d*WormholeExtent,layers))fragColor=vec4(missing(d),1);return;}
    if(PortalOpen>.5)for(int i=0;i<2;i++) {
        vec3 v=p-localMouth(i);float rr=dot(v,v);
        if(rr<mouth*mouth) {
            if(rr<1e-10){v=-d*.0001;rr=dot(v,v);}
            p=localMouth(1-i)+wormholeReflect(v)*(mouth*mouth/rr);
            d=normalize(wormholeReflect(d-2.0*v*dot(v,d)/rr));passages=1;break;
        }
    }
    vec3 offset=p-MassSource;float r=length(offset);
    mixedInterior=MassRadius>0.0 && MassBodyRadius==0.0 && r<=MassRadius;
    if(mixedInterior) {
        mixedEditor=true;float along=dot(offset,d);
        float reach=max(0.0,-along+sqrt(max(0.0,along*along+MassRadius*MassRadius-r*r)));
        if(localSceneChord(p,p+d*reach,layers))return;
        mixedEditor=false;wormholeTravelled=0.0;
        for(int tree=0;tree<SCENE_TREES;tree++)cellKnown[tree]=false;
        if(r<=.1*MassRadius){diagnostic=vec4(0,0,0,-1);fragColor=vec4(0,0,0,1);return;}
    }
    // Same static/falling observer convention as the established mass renderer.
    if(MassRadius>0.0 && r>1e-7) {
        vec3 n=offset/r;float mu=dot(n,d);vec3 t=d-mu*n;
        if(MassBodyRadius>0.0) {
            float b=r>=MassBodyRadius?1.0-MassRadius/r:1.0-MassRadius*r*r/pow(MassBodyRadius,3.0);
            d=normalize(n*mu*sqrt(mix(1.0,b,mixedWindow(r,mixedMassExtent())))+t);
        } else {
            float flow=sqrt(MassRadius/r*mixedWindow(r,mixedMassExtent()));
            float boost=min(.999999,flow*smoothstep(1.05,1.25,r/MassRadius));
            float fm=clamp((mu-boost)/(1.0-boost*mu),-1.0,1.0);
            float fs=length(t)*sqrt(1.0-boost*boost)/(1.0-boost*mu),energy=1.0+flow*fm;
            float impact=fs/(max(MassRadius/r,1e-12)*energy);
            // An isolated capture cone cannot classify exterior mixed rays:
            // another field or a portal can redirect them before they hit the BH.
            skyAllowed=!mixedInterior || energy>0.0 && fm+flow>0.0 && impact<2.598076211;
            if(length(t)<1e-5 && !skyAllowed){diagnostic=vec4(0,0,0,-1);fragColor=vec4(0,0,0,1);return;}
            vec3 coordinateDirection=n*(fm+flow)+(length(t)>1e-7?t*fs/length(t):vec3(0));
            if(length(coordinateDirection)<1e-8){diagnostic=vec4(0,0,0,-1);fragColor=vec4(0,0,0,1);return;}
            d=normalize(coordinateDirection);
        }
    }
    for(int iteration=0;iteration<2048;iteration++) {
        float radius=length(p-MassSource),nextRegion=1e20;
        bool outside=MassRadius<=0.0 || radius>mixedMassExtent()+.001;
        if(MassRadius>0.0)nextRegion=mixedEntry(p,d,MassSource,mixedMassExtent());
        for(int i=0;i<2;i++) {
            if(float(i)>=PortalCount)break;
            outside=outside && length(p-localMouth(i))>WormholeInfluence+.001;
            nextRegion=min(nextRegion,mixedEntry(p,d,localMouth(i),WormholeInfluence));
        }
        if(outside) {
            vec3 end=p+d*min(nextRegion,WormholeExtent);
            if(localSceneChord(p,end,layers))return;
            if(nextRegion>=WormholeExtent) {
                diagnostic=vec4(d,float(passages));fragColor=skyAllowed?vec4(missing(d),1):vec4(0,0,0,1);return;
            }
            p=end;continue;
        }
        vec3 acceleration=mixedAcceleration(p,d);
        float tolerance=.001*(PathStep/.45)*(PathStep/.45)*CurveFactor;
        float h=clamp(sqrt(8.0*tolerance/max(length(acceleration),1e-12)),.02,MeshStepLimit);
        if(MassRadius>0.0 && radius<mixedMassExtent())h=min(h,max(.02,mixedRadialStep(p-MassSource,d)));
        if(MassBodyRadius>0.0)h=min(h,max(.0005,.25*abs(radius-MassBodyRadius)));
        for(int i=0;i<2;i++)if(float(i)<PortalCount && length(p-localMouth(i))<WormholeInfluence)
            h=min(h,max(.02,mixedRadialStep(p-localMouth(i),d)));
        vec3 end,nextD;mixedStep(p,d,h,end,nextD);
        int contact=-1;float fraction=2.0;
        for(int i=0;i<2;i++) {
            if(float(i)>=PortalCount)break;
            float f=mixedContact(p,end,localMouth(i),PortalOpen>.5?mouth:ClosedRadius);
            if(f<fraction){fraction=f;contact=i;}
        }
        if(MassRadius>0.0 && MassBodyRadius==0.0) {
            float f=mixedContact(p,end,MassSource,MassRadius*(mixedInterior?.1:1.0));
            if(f<fraction){fraction=f;contact=2;}
        }
        bool touched=contact>=0 && fraction<=1.0;
        if(touched) {
            vec3 centre=contact==2?MassSource:localMouth(contact);
            float boundary=contact==2?MassRadius*(mixedInterior?.1:1.0):PortalOpen>.5?mouth:ClosedRadius;
            float part=h*max(0.0,fraction);
            for(int root=0;root<4;root++) {
                mixedStep(p,d,part,end,nextD);vec3 v=end-centre;float rr=length(v),speed=dot(v,nextD)/max(rr,1e-8);
                if(abs(speed)>1e-6)part=clamp(part-(rr-boundary)/speed,0.0,h);
            }
            h=part;mixedStep(p,d,h,end,nextD);end=centre+normalize(end-centre)*boundary;
        }
        if(localSceneChord(p,end,layers))return;
        turn+=h*length(acceleration);p=end;d=nextD;returningBody=turn>3.14159265 || passages>0;
        if(touched) {
            if(contact==2 || PortalOpen<.5){diagnostic=vec4(0,0,0,-1);fragColor=vec4(0,0,0,1);return;}
            if(passages>=4){diagnostic=vec4(0,0,0,-3);fragColor=vec4(0,0,0,1);return;}
            vec3 n=normalize(p-localMouth(contact));
            p=localMouth(1-contact)+mouth*wormholeReflect(n);
            d=normalize(wormholeReflect(d-2.0*n*dot(n,d)));passages++;skyAllowed=true;
            for(int tree=0;tree<SCENE_TREES;tree++)cellKnown[tree]=false;
        }
        if(turn>25.1327412){diagnostic=vec4(0,0,0,-3);fragColor=vec4(0,0,0,1);return;}
    }
    diagnostic=vec4(0,0,0,-2);fragColor=vec4(.7,.05,.5,1);
}
