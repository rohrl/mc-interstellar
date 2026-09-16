#version 150
uniform sampler2D Voxels;
uniform sampler2D Palette;
uniform sampler2D Atlas;
uniform sampler2D Background;
uniform vec2 Viewport;
uniform vec3 Camera,Source,Forward,Right,Up;
uniform float Radius,Lensing,PathStep,Diagnostic,LiveBackground;
vec4 diagnostic=vec4(0);
ivec3 materialCell;
in vec2 screenUv;
out vec4 fragColor;
const int SIDE=96;

vec3 missing(vec3 direction) {
    // Live-only same-screen fallback: distant scenery is visible, but is not lensed.
    // The world framebuffer is read while a separate terrain target is bound.
    if(LiveBackground>.5) return texture(Background,vec2(screenUv.x,1.0-screenUv.y)).rgb;
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
    if(value==1) return vec3(.85,.45,.06);
    if(value==2) return vec3(.7,.05,.5);
    int face=abs(normal.x)>.5?(normal.x<0?4:5):(abs(normal.y)>.5?(normal.y<0?0:1):(normal.z<0?2:3));
    vec3 local=clamp(hit-vec3(materialCell),vec3(.0001),vec3(.9999));
    vec2 st=abs(normal.x)>.5?local.zy:(abs(normal.y)>.5?local.xz:local.xy);
    vec3 u=texelFetch(Palette,ivec2(face*3,value),0).xyz;
    vec3 v=texelFetch(Palette,ivec2(face*3+1,value),0).xyz;
    vec3 tint=texelFetch(Palette,ivec2(face*3+2,value),0).rgb;
    vec2 uv=vec2(dot(u,vec3(st,1)),dot(v,vec3(st,1)));
    vec3 albedo=textureLod(Atlas,uv,0).rgb*tint;
    float light=.55+.45*max(0.0,dot(normal,normalize(vec3(-.4,.8,-.3))));
    return albedo*light;
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
        float distance=400.0;
        bool horizon=Lensing>.5 && mu<0.0;
        if(horizon) distance=r-Radius;
        int value=segment(Camera,Camera+direction*distance,hit,normal);
        fragColor=vec4(value>0?surface(value,hit,normal):(horizon&&value==-1?dark():missing(direction)),1);return;
    }
    vec3 tangentAxis=tangentVector/tangent;
    float u=Radius/r;
    vec2 q=vec2(u,-mu*u*sqrt(1.0-u)/tangent);
    vec3 p=Camera;float phi=0.0;
    for(int i=0;i<2048;i++) {
        // Once outgoing beyond the sphere enclosing all data, no future chord can re-enter.
        if(q.y<0.0 && Radius/q.x>max(1.5*Radius,length(max(abs(Source),abs(vec3(SIDE)-Source))))) {
            fragColor=vec4(missing(normalize(p-Source)),1);return;
        }
        float speed=Radius*length(q)/(q.x*q.x);
        float h=min(.02,PathStep/max(speed,.0001));
        vec2 a=derivative(q),b=derivative(q+h*a*.5),c=derivative(q+h*b*.5),d=derivative(q+h*c);
        vec2 next=q+h*(a+2.0*b+2.0*c+d)/6.0;
        bool captured=next.x>=1.0;
        float angle=phi+h;
        if(captured) {angle=phi+h*(1.0-q.x)/(next.x-q.x);next.x=1.0;}
        if(next.x<=0.0) {fragColor=vec4(missing(direction),1);return;}
        vec3 end=Source+(Radius/next.x)*(cos(angle)*radialAxis+sin(angle)*tangentAxis);
        int value=segment(p,end,hit,normal);
        if(value>0) {fragColor=vec4(length(hit-Source)<Radius?dark():surface(value,hit,normal),1);return;}
        if(value==0) {fragColor=vec4(missing(normalize(end-p)),1);return;}
        if(captured) {fragColor=vec4(dark(),1);return;}
        phi=angle;q=next;p=end;
        if(phi>=16.0) break;
    }
    diagnostic=vec4(0,0,0,-2);
    fragColor=vec4(.7,.05,.5,1);
}
void main() {trace();if(Diagnostic>.5)fragColor=diagnostic;}
