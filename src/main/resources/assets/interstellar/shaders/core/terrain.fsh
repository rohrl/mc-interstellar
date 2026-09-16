#version 150
uniform sampler2D Voxels;
uniform sampler2D Palette;
uniform sampler2D Atlas;
uniform sampler2D Distant;
uniform float Hybrid;
uniform float DistantTop;
uniform vec3 SkyColor;
uniform vec2 Viewport;
uniform vec3 Camera,Source,Forward,Right,Up;
uniform float Radius,Lensing,PathStep,Diagnostic;
vec4 diagnostic=vec4(0);
ivec3 materialCell;
bool distantHit=false;
in vec2 screenUv;
out vec4 fragColor;
const int SIDE=96;

vec3 missing(vec3 direction) {
    if(Hybrid>.5 && Diagnostic<.5) return SkyColor;
    float grid=step(.92,fract(atan(direction.x,direction.z)*8.0))+step(.92,fract(asin(clamp(direction.y,-1,1))*8.0));
    return mix(vec3(.055,.085,.12),vec3(.28,.19,.07),min(grid,1.0));
}
vec3 dark() {diagnostic=vec4(0,0,0,-1);return vec3(0);}

// Exact voxel traversal along one straight chord. -1=clear segment, 0=left snapshot, >0=material.
int segment(vec3 start,vec3 end,out vec3 hit,out vec3 normal) {
    vec3 d=end-start;
    // Clip this chord to the data box. A chord outside may enter on a later orbit step.
    float enter=0.0,leave=1.0;
    normal=vec3(0,1,0);
    for(int axis=0;axis<3;axis++) {
        if(abs(d[axis])<1e-12) {
            if(start[axis]<0.0 || start[axis]>=float(SIDE)) return -1;
        } else {
            float a=-start[axis]/d[axis],b=(float(SIDE)-start[axis])/d[axis];
            float nearT=min(a,b);
            if(nearT>enter) {enter=nearT;normal=vec3(0);normal[axis]=-sign(d[axis]);}
            leave=min(leave,max(a,b));
        }
    }
    if(leave<=enter) return -1;
    ivec3 cell=clamp(ivec3(floor(start+enter*d)),ivec3(0),ivec3(SIDE-1));
    ivec3 stepDirection=ivec3(sign(d));
    vec3 dt=vec3(1e30),next=vec3(1e30);
    for(int axis=0;axis<3;axis++) if(abs(d[axis])>1e-12) {
        dt[axis]=abs(1.0/d[axis]);
        next[axis]=(float(cell[axis])+(d[axis]>0.0?1.0:0.0)-start[axis])/d[axis];
    }
    float t=enter;
    for(int i=0;i<384;i++) {
        hit=start+t*d;
        if(any(lessThan(cell,ivec3(0))) || any(greaterThanEqual(cell,ivec3(SIDE)))) return 0;
        materialCell=cell;
        int value=int(texelFetch(Voxels,ivec2(cell.x+cell.z*SIDE,cell.y),0).r+.5);
                if(value!=0) {
            float height=value<3?1.0:texelFetch(Palette,ivec2(0,value),0).w;
            if(height>=1.0) return value;
            // Intersect the actual snow cuboid inside this voxel/chord, including its top.
            vec3 lower=vec3(cell),upper=lower+vec3(1,height,1);
            float enter=0.0,leave=1.0;vec3 faceNormal=normal;
            bool intersects=true;
            for(int a=0;a<3;a++) {
                if(abs(d[a])<1e-12) {
                    if(start[a]<lower[a] || start[a]>=upper[a]) intersects=false;
                } else {
                    float t0=(lower[a]-start[a])/d[a],t1=(upper[a]-start[a])/d[a];
                    float nearT=min(t0,t1);
                    if(nearT>enter) {enter=nearT;faceNormal=vec3(0);faceNormal[a]=-sign(d[a]);}
                    leave=min(leave,max(t0,t1));
                }
            }
            if(intersects && leave>=enter && enter>=t-1e-6) {
                hit=start+enter*d;normal=faceNormal;return value;
            }
        }
        int axis=next.x<next.y?(next.x<next.z?0:2):(next.y<next.z?1:2);
        t=next[axis];
        if(t>1.0) return -1;
        cell[axis]+=stepDirection[axis];next[axis]+=dt[axis];
        normal=vec3(0);normal[axis]=-float(stepDirection[axis]);
    }
    return 2;
}
vec3 surface(int value,vec3 hit,vec3 normal) {
    diagnostic=vec4(vec3(materialCell),float(value));
    if(value==-1) return vec3(.08,.22,.32);
    if(value==1) return vec3(.85,.45,.06);
    if(value==2) return vec3(.7,.05,.5);
    int face=abs(normal.x)>.5?(normal.x<0?4:5):(abs(normal.y)>.5?(normal.y<0?0:1):(normal.z<0?2:3));
    vec3 local=distantHit?fract(hit):clamp(hit-vec3(materialCell),vec3(.0001),vec3(.9999));
    vec2 st=abs(normal.x)>.5?local.zy:(abs(normal.y)>.5?local.xz:local.xy);
    vec3 u=texelFetch(Palette,ivec2(face*3,value),0).xyz;
    vec3 v=texelFetch(Palette,ivec2(face*3+1,value),0).xyz;
    vec3 tint=texelFetch(Palette,ivec2(face*3+2,value),0).rgb;
    vec2 uv=vec2(dot(u,vec3(st,1)),dot(v,vec3(st,1)));
    vec3 albedo=textureLod(Atlas,uv,0).rgb*tint;
    float light=.55+.45*max(0.0,dot(normal,normalize(vec3(-.4,.8,-.3))));
    vec3 colour=albedo*light;
    return distantHit?mix(colour,SkyColor,smoothstep(80.0,128.0,length((hit-Source).xz))):colour;
}

// Intersect one height-field column prism. Near volume is never represented here.
bool columnHit(vec3 start,vec3 delta,vec3 lower,vec3 upper,float from,float until,
               out float at,out vec3 normal) {
    at=from;float leave=until;normal=vec3(0,1,0);
    for(int a=0;a<3;a++) {
        if(abs(delta[a])<1e-10) {if(start[a]<lower[a] || start[a]>=upper[a])return false;}
        else {
            float t0=(lower[a]-start[a])/delta[a],t1=(upper[a]-start[a])/delta[a];
            if(min(t0,t1)>at) {at=min(t0,t1);normal=vec3(0);normal[a]=-sign(delta[a]);}
            leave=min(leave,max(t0,t1));
        }
    }
    return leave>=at;
}
int distantSegment(vec3 start,vec3 end,out vec3 hit,out vec3 normal) {
    // Conservative whole-field height rejection, including the clipped local columns.
    if(min(start.y,end.y)>DistantTop)return 0;
    vec3 delta=end-start;float enter=0.0,leave=1.0;
    for(int a=0;a<3;a+=2) {
        if(abs(delta[a])<1e-10) {if(start[a]<-80.0 || start[a]>=176.0)return 0;}
        else {
            float t0=(-80.0-start[a])/delta[a],t1=(176.0-start[a])/delta[a];
            enter=max(enter,min(t0,t1));leave=min(leave,max(t0,t1));
        }
    }
    if(leave<=enter)return 0;
    ivec2 cell=clamp(ivec2(floor((start+enter*delta).xz)),ivec2(-80),ivec2(175));
    ivec2 stepDirection=ivec2(sign(delta.xz));
    vec2 dt=vec2(1e30),next=vec2(1e30);
    for(int a=0;a<2;a++) if(abs(delta.xz[a])>1e-10) {
        dt[a]=abs(1.0/delta.xz[a]);
        next[a]=(float(cell[a])+(delta.xz[a]>0?1.0:0.0)-start.xz[a])/delta.xz[a];
    }
    float t=enter;
    for(int i=0;i<520;i++) {
        if(any(lessThan(cell,ivec2(-80))) || any(greaterThanEqual(cell,ivec2(176))))return 0;
        vec4 field=texelFetch(Distant,cell+ivec2(80),0);
        bool local=all(greaterThanEqual(cell,ivec2(0))) && all(lessThan(cell,ivec2(SIDE)));
        float until=min(leave,min(next.x,next.y)),best=2.0;int value=0;vec3 bestNormal;
        for(int layer=0;layer<2;layer++) {
            if(layer==1 && !local)break;
            vec2 sampleValue=layer==0?field.xy:field.zw;
            int id=int(sampleValue.y);if(id==0 || id==1)continue;
            float lower=local && layer==0?float(SIDE):-1024.0;
            float upper=local && layer==1?min(0.0,sampleValue.x):sampleValue.x;
            if(upper<=lower)continue;
            float at;vec3 n;
            if(columnHit(start,delta,vec3(cell.x,lower,cell.y),vec3(cell.x+1,upper,cell.y+1),t,until,at,n) && at<best) {
                best=at;value=id;bestNormal=n;
            }
        }
        if(value!=0) {hit=start+best*delta;normal=bestNormal;materialCell=ivec3(cell.x,int(floor(hit.y)),cell.y);return value;}
        int axis=next.x<next.y?0:1;t=next[axis];if(t>=leave)return 0;
        cell[axis]+=stepDirection[axis];next[axis]+=dt[axis];
    }
    return 2;
}
int sceneSegment(vec3 start,vec3 end,out vec3 hit,out vec3 normal) {
    distantHit=false;
    int local=segment(start,end,hit,normal);
    if(Hybrid<.5 || Diagnostic>.5)return local;
    ivec3 savedCell=materialCell;vec3 farHit,farNormal;
    vec3 limit=local>0?hit:end;
    if(all(greaterThanEqual(start,vec3(0))) && all(lessThan(start,vec3(SIDE))) &&
       all(greaterThanEqual(limit,vec3(0))) && all(lessThan(limit,vec3(SIDE))))return local;
    int farValue=distantSegment(start,limit,farHit,farNormal);
    if(farValue!=0) {distantHit=true;hit=farHit;normal=farNormal;return farValue;}
    materialCell=savedCell;return local;
}
vec2 derivative(vec2 q) {return vec2(q.y,1.5*q.x*q.x-q.x);}
void trace() {
    vec2 xy=(screenUv*2.0-1.0)*.7002075382;xy.x*=Viewport.x/Viewport.y;
    vec3 direction=normalize(Forward+xy.x*Right-xy.y*Up);
    vec3 hit,normal;
    vec3 radialAxis=normalize(Camera-Source);
    float r=length(Camera-Source),mu=dot(direction,radialAxis);
    vec3 tangentVector=direction-mu*radialAxis;
    float tangent=length(tangentVector);
    if(Lensing<.5 || tangent<1e-5) {
        float distance=Hybrid>.5 && Diagnostic<.5?1024.0:400.0;
        bool horizon=Lensing>.5 && mu<0.0;
        if(horizon) distance=r-Radius;
        int value=sceneSegment(Camera,Camera+direction*distance,hit,normal);
        fragColor=vec4((value>0 || distantHit)?surface(value,hit,normal):(horizon&&value==-1?dark():missing(direction)),1);return;
    }
    vec3 tangentAxis=tangentVector/tangent;
    float u=Radius/r;
    vec2 q=vec2(u,-mu*u*sqrt(1.0-u)/tangent);
    vec3 p=Camera;float phi=0.0;
    for(int i=0;i<2048;i++) {
        // Prototype heuristic: retain the curved outgoing direction, then use a
        // straight far continuation beyond both the local sphere and 12 r_s.
        // This omits remaining weak-field deflection; never paste camera pixels.
        if(Hybrid>.5 && Diagnostic<.5 && q.y<0.0 && Radius/q.x>max(96.0,12.0*Radius)) {
            vec3 radial=cos(phi)*radialAxis+sin(phi)*tangentAxis;
            vec3 angular=-sin(phi)*radialAxis+cos(phi)*tangentAxis;
            vec3 outgoing=normalize(-q.y*radial+q.x*angular),farHit,farNormal;
            int farValue=distantSegment(p,p+outgoing*1024.0,farHit,farNormal);
            distantHit=farValue!=0;
            fragColor=vec4(distantHit?surface(farValue,farHit,farNormal):missing(outgoing),1);return;
        }
        // Once outgoing beyond the sphere enclosing all data, no future chord can re-enter.
        if(q.y<0.0 && Radius/q.x>(Hybrid>.5 && Diagnostic<.5?512.0:max(1.5*Radius,length(max(abs(Source),abs(vec3(SIDE)-Source)))))) {
            fragColor=vec4(missing(normalize(p-Source)),1);return;
        }
        float speed=Radius*length(q)/(q.x*q.x);
        float stepSize=PathStep;
        if(Hybrid>.5 && Diagnostic<.5) stepSize=mix(PathStep,4.0,smoothstep(80.0,144.0,length(p-Source)));
        float h=min(.02,stepSize/max(speed,.0001));
        vec2 a=derivative(q),b=derivative(q+h*a*.5),c=derivative(q+h*b*.5),d=derivative(q+h*c);
        vec2 next=q+h*(a+2.0*b+2.0*c+d)/6.0;
        bool captured=next.x>=1.0;
        float angle=phi+h;
        if(captured) {angle=phi+h*(1.0-q.x)/(next.x-q.x);next.x=1.0;}
        if(next.x<=0.0) {fragColor=vec4(missing(direction),1);return;}
        vec3 end=Source+(Radius/next.x)*(cos(angle)*radialAxis+sin(angle)*tangentAxis);
        int value=sceneSegment(p,end,hit,normal);
        if(value>0 || distantHit) {fragColor=vec4(length(hit-Source)<Radius?dark():surface(value,hit,normal),1);return;}
        if(value==0 && (Hybrid<.5 || Diagnostic>.5)) {fragColor=vec4(missing(normalize(end-p)),1);return;}
        if(captured) {fragColor=vec4(dark(),1);return;}
        phi=angle;q=next;p=end;
        if(phi>=16.0) break;
    }
    diagnostic=vec4(0,0,0,-2);
    fragColor=vec4(.7,.05,.5,1);
}
void main() {
    if(Diagnostic>1.5) {
        vec2 xy=(screenUv*2.0-1.0)*.7002075382;xy.x*=Viewport.x/Viewport.y;
        vec3 d=normalize(Forward+xy.x*Right-xy.y*Up),hit,normal;
        int value=distantSegment(Camera,Camera+d*1024.0,hit,normal);
        fragColor=vec4(value==0?0.0:length(hit-Camera),0,0,float(value));return;
    }
    trace();if(Diagnostic>.5)fragColor=diagnostic;
}
