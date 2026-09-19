#version 150
uniform sampler2D Voxels;
uniform sampler2D Palette;
uniform sampler2D Atlas;
uniform sampler2D Distant;
uniform sampler2D DistantAppearance,SkyAtlas,Lightmap;
uniform sampler2D LocalLight,DistantLight;
uniform sampler2D SmoothAtlas,LocalSmooth,DistantSmooth;
// Mesh and voxel backends are mutually exclusive; stay within Minecraft's 12 texture units.
#define MeshTriangles Voxels
#define MeshNodes Palette
uniform float MeshMode,MeshNodeCount;
uniform float MeshEntities;
uniform float MeshClouds,MeshExtent;
uniform float MeshCoverage,MovingNodeCount;
uniform vec4 OldMeshBounds;
#define EntityAtlas LocalLight
#define CloudAtlas Distant
vec4 cloudLayer=vec4(0);
vec3 meshColour;
uniform vec4 FaceShades;
uniform float Hybrid;
uniform float FaceLighting;
uniform float SmoothLighting;
uniform float DistantTop;
uniform vec4 ViewSlopes;
uniform vec3 TerrainFogRange;
uniform vec4 TerrainFogColour;
uniform vec3 Camera,Source,Forward,Right,Up;
uniform float Radius,Lensing,PathStep,Diagnostic;
uniform vec2 Viewport;
uniform float RaySamples;
uniform float AdaptivePath;
uniform float FastBounds;
uniform float FastFetch;
vec4 diagnostic=vec4(0);
ivec3 materialCell;
bool distantHit=false;
int distantLayer=0;
bool distantSide=false;
vec2 surfaceLight=vec2(0,15);
in vec2 screenUv;
out vec4 fragColor;
const int SIDE=96;

vec3 nativeSky(vec3 d) {
    vec3 a=abs(d),forward,up;int face;
    if(a.x>=a.y && a.x>=a.z) {face=d.x>0?0:1;forward=vec3(sign(d.x),0,0);up=vec3(0,1,0);}
    else if(a.y>=a.z) {face=d.y>0?2:3;forward=vec3(0,sign(d.y),0);up=vec3(0,0,sign(d.y));}
    else {face=d.z>0?4:5;forward=vec3(0,0,sign(d.z));up=vec3(0,1,0);}
    vec2 uv=vec2(dot(d,cross(forward,up)),dot(d,up))/dot(d,forward)*.5+.5;
    uv=clamp(uv,vec2(.5/256.0),vec2(255.5/256.0));
    return texture(SkyAtlas,vec2((float(face)+uv.x)/6.0,uv.y)).rgb;
}
vec3 worldLight() {return texture(Lightmap,(surfaceLight+.5)/16.0).rgb;}

vec3 missing(vec3 direction) {
    if(Hybrid>.5 && Diagnostic<.5) return nativeSky(direction);
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
vec3 cornerLight(float encoded) {
    int code=int(abs(encoded));
    vec2 uv=(vec2(code&255,(code>>8)&255)+8.0)/256.0;
    return texture(Lightmap,uv).rgb*(float(code>>16)/255.0);
}
vec3 smoothLight(int id,int face,vec2 st) {
    vec4 data=texelFetch(SmoothAtlas,ivec2((id%128)*6+face,id/128),0);
    vec4 weights;
    if(data.x<0.0) weights=st.y<=st.x?vec4(1-st.x,st.x-st.y,0,st.y):vec4(1-st.y,0,st.y-st.x,st.x);
    else weights=st.x+st.y<=1?vec4(1-st.x-st.y,st.x,st.y,0):vec4(0,1-st.y,1-st.x,st.x+st.y-1);
    return cornerLight(data.x)*weights.x+cornerLight(data.y)*weights.y+cornerLight(data.z)*weights.z+cornerLight(data.w)*weights.w;
}
vec4 meshData(sampler2D data,int index) {
    int width=textureSize(data,0).x;
    // Known atlas widths let the compiler replace variable integer division.
    // Keep integer coordinates and a general fallback; no float-address rounding.
    if(FastFetch>.5) {
        if(width==4095)return texelFetch(data,ivec2(index%4095,index/4095),0);
        if(width==4096)return texelFetch(data,ivec2(index%4096,index/4096),0);
    }
    return texelFetch(data,ivec2(index%width,index/width),0);
}
float cloudFogDistance(vec3 position) {
    vec3 d=position-Camera;
    // Native cloud vertex shader measures fog after the view transform.
    vec3 view=vec3(dot(d,Right),dot(d,Up),dot(d,Forward));
    return TerrainFogRange.z>.5?max(length(view.xz),abs(view.y)):length(view);
}
// The live actor tree shares nearest-hit and cloud ordering with the retained terrain tree.
vec4 sceneTriangle(int tree,int index) {return tree==0?meshData(MeshTriangles,index):meshData(DistantAppearance,index);}
vec4 sceneNode(int tree,int index) {return tree==0?meshData(MeshNodes,index):meshData(DistantLight,index);}
// Stackless preorder traversal: escape links skip whole subtrees. Each chord has one nearest hit.
int meshSegment(vec3 start,vec3 end,out vec3 hit,out vec3 normal) {
    vec3 delta=end-start;float best=1.000001;bool found=false;
    // One reciprocal per chord, shared by both BVHs. Parallel axes use finite
    // placeholders and explicit containment, avoiding zero-times-infinity NaNs.
    bvec3 parallel=lessThan(abs(delta),vec3(1e-12));
    vec3 inverseDelta=1.0/mix(delta,vec3(1),parallel);
    float cloudAt=2.0;vec4 nearestCloud=vec4(0);
    for(int tree=0;tree<2;tree++) {
    int node=0,nodeCount=int(tree==0?MeshNodeCount:MovingNodeCount),returnTo=0;
    for(int visited=0;visited<131072;visited++) {
        if(node<0)node=returnTo;
        if(node==nodeCount)break;
        vec4 lower=sceneNode(tree,node*3),upper=sceneNode(tree,node*3+1);
        float enter=0,leave=min(1.0,best);bool inside=true;
        if(FastBounds>.5) {
            vec3 low=lower.xyz-vec3(.00001),high=upper.xyz+vec3(.00001);
            vec3 a=(low-start)*inverseDelta,b=(high-start)*inverseDelta;
            vec3 nearT=mix(min(a,b),vec3(0),parallel);
            vec3 farT=mix(max(a,b),vec3(1),parallel);
            enter=max(0.0,max(nearT.x,max(nearT.y,nearT.z)));
            leave=min(leave,min(farT.x,min(farT.y,farT.z)));
            bvec3 outside=bvec3(start.x<low.x || start.x>high.x,
                start.y<low.y || start.y>high.y,start.z<low.z || start.z>high.z);
            inside=!(parallel.x && outside.x || parallel.y && outside.y || parallel.z && outside.z);
        } else for(int axis=0;axis<3;axis++) {
            if(abs(delta[axis])<1e-12) {
                if(start[axis]<lower[axis]-.00001 || start[axis]>upper[axis]+.00001)inside=false;
            } else {
                float a=(lower[axis]-.00001-start[axis])/delta[axis],b=(upper[axis]+.00001-start[axis])/delta[axis];
                enter=max(enter,min(a,b));leave=min(leave,max(a,b));
            }
        }
        if(!inside || leave<enter) {node=int(lower.w);continue;}
        int count=int(sceneNode(tree,node*3+2).x);
        if(count<0) {returnTo=int(lower.w);node=int(upper.w);continue;}
        for(int i=0;i<count;i++) {
            int base=(int(upper.w)+i)*9;
            vec4 vertexA=sceneTriangle(tree,base);
            float entity=vertexA.w;
            bool cloud=entity>=5.0;
            if(cloud && (MeshClouds<.5 || cloudLayer.a>0.0))continue;
            if(!cloud && entity!=0.0 && MeshEntities<.5)continue;
            vec3 a=vertexA.xyz,b=sceneTriangle(tree,base+3).xyz,c=sceneTriangle(tree,base+6).xyz;
            vec3 edge1=b-a,edge2=c-a,p=cross(delta,edge2);
            bool twoSided=abs(entity)==2.0 || entity==6.0;
            float det=dot(edge1,p);if(twoSided?abs(det)<1e-10:det<1e-10)continue;
            vec3 s=start-a;float u=dot(s,p)/det;if(u<0 || u>1)continue;
            vec3 q=cross(s,edge1);float v=dot(delta,q)/det;if(v<0 || u+v>1)continue;
            float t=dot(edge2,q)/det;if(t<0 || t>1 || t>=best)continue;
            // U isolates terrain missing beyond the previous source-centred footprint.
            vec2 location=(start+delta*t).xz;
            if(entity==0.0 && MeshCoverage<.5 && (any(lessThan(location,OldMeshBounds.xy)) || any(greaterThanEqual(location,OldMeshBounds.zw))))continue;
            vec3 weights=vec3(1-u-v,u,v);
            vec4 uvA=sceneTriangle(tree,base+1),uvB=sceneTriangle(tree,base+4),uvC=sceneTriangle(tree,base+7);
            if(entity==0.0) {
                // K retains the old half-texel offset for controlled appearance comparisons.
                vec2 offset=vec2(FaceLighting>.5?0.0:.5/16.0);
                uvA.zw=clamp(uvA.zw+offset,vec2(.5/16.0),vec2(15.5/16.0));
                uvB.zw=clamp(uvB.zw+offset,vec2(.5/16.0),vec2(15.5/16.0));
                uvC.zw=clamp(uvC.zw+offset,vec2(.5/16.0),vec2(15.5/16.0));
            }
            vec2 uv=uvA.xy*weights.x+uvB.xy*weights.y+uvC.xy*weights.z;
            if(cloud) {
                if(t>=cloudAt)continue;
                vec4 colour=textureLod(CloudAtlas,uv,0)*(sceneTriangle(tree,base+2)*weights.x+
                        sceneTriangle(tree,base+5)*weights.y+sceneTriangle(tree,base+8)*weights.z);
                if(colour.a<.1)continue;
                float distance=dot(vec3(cloudFogDistance(a),cloudFogDistance(b),cloudFogDistance(c)),weights);
                float fog=TerrainFogRange.y>TerrainFogRange.x?smoothstep(TerrainFogRange.x,TerrainFogRange.y,distance):step(TerrainFogRange.y,distance);
                colour.rgb=mix(colour.rgb,TerrainFogColour.rgb,fog*TerrainFogColour.a);
                nearestCloud=colour;cloudAt=t;continue;
            }
            vec4 texel=Diagnostic>2.5?vec4(1):entity>0.0?textureLod(EntityAtlas,uv,0):textureLod(Atlas,uv,0);
            if(texel.a<.1)continue;
            vec3 colA=sceneTriangle(tree,base+2).rgb*texture(Lightmap,uvA.zw).rgb;
            vec3 colB=sceneTriangle(tree,base+5).rgb*texture(Lightmap,uvB.zw).rgb;
            vec3 colC=sceneTriangle(tree,base+8).rgb*texture(Lightmap,uvC.zw).rgb;
            meshColour=texel.rgb*(colA*weights.x+colB*weights.y+colC*weights.z);
            best=t;hit=start+t*delta;normal=normalize(cross(edge1,edge2));found=true;
        }
        node=count>0?int(lower.w):node+1;
    }
    if(node!=nodeCount) {diagnostic=vec4(0,0,0,-2);meshColour=vec3(1,0,1);hit=start;normal=vec3(0,1,0);return 3;}
    }
    // Vanilla fancy clouds use a depth prepass: blend the nearest cloud surface once.
    if(cloudAt<best && cloudLayer.a==0.0)cloudLayer=nearestCloud;
    return found?3:-1;
}
vec3 surface(int value,vec3 hit,vec3 normal) {
    if(MeshMode>.5) {
        // Synthetic opaque-box fixture: report the entered cell, preserving traversal failures.
        if(Diagnostic>2.5) {if(diagnostic.w!=-2.0)diagnostic=vec4(floor(hit-normal*.001),3);return vec3(0);}
        vec3 relative=hit-Camera;
        float fogDistance=TerrainFogRange.z>.5?max(length(relative.xz),abs(relative.y)):length(relative);
        float amount=TerrainFogRange.y>TerrainFogRange.x?smoothstep(TerrainFogRange.x,TerrainFogRange.y,fogDistance):step(TerrainFogRange.y,fogDistance);
        return mix(meshColour,TerrainFogColour.rgb,amount*TerrainFogColour.a);
    }
    diagnostic=vec4(vec3(materialCell),float(value));
    if(value==-1) return vec3(.08,.22,.32)*worldLight();
    if(value==1) return vec3(.85,.45,.06);
    if(value==2) return vec3(.7,.05,.5);
    int face=abs(normal.x)>.5?(normal.x<0?4:5):(abs(normal.y)>.5?(normal.y<0?0:1):(normal.z<0?2:3));
    if(Hybrid>.5 && FaceLighting>.5) {
        vec2 codes;
        if(distantHit) {
            ivec2 column=materialCell.xz+ivec2(80);
            vec4 layers=texelFetch(DistantLight,ivec2(column.x*2+distantLayer,column.y),0);
            codes=distantSide?layers.zw:layers.xy;
        } else codes=texelFetch(LocalLight,ivec2(materialCell.x+materialCell.z*SIDE,materialCell.y),0).rg;
        int lightCode=(int(codes[face/3])>>((face%3)*8))&255;
        surfaceLight=vec2(lightCode&15,lightCode>>4);
    }
    vec3 local=distantHit?fract(hit):clamp(hit-vec3(materialCell),vec3(.0001),vec3(.9999));
    vec2 st=abs(normal.x)>.5?local.zy:(abs(normal.y)>.5?local.xz:local.xy);
    vec3 u=texelFetch(Palette,ivec2(face*3,value),0).xyz;
    vec3 v=texelFetch(Palette,ivec2(face*3+1,value),0).xyz;
    vec3 tint=texelFetch(Palette,ivec2(face*3+2,value),0).rgb;
    vec2 uv=vec2(dot(u,vec3(st,1)),dot(v,vec3(st,1)));
    vec3 albedo=textureLod(Atlas,uv,0).rgb*tint;
    float light=Hybrid>.5?(normal.y>.5?FaceShades.w:normal.y<-.5?FaceShades.z:abs(normal.x)>.5?FaceShades.x:FaceShades.y):
            .55+.45*max(0.0,dot(normal,normalize(vec3(-.4,.8,-.3))));
    vec3 colour=albedo*light*(Hybrid>.5?worldLight():vec3(1));
    if(Hybrid>.5 && FaceLighting>.5 && SmoothLighting>.5) {
        int id;
        if(distantHit) {
            vec4 ids=texelFetch(DistantSmooth,materialCell.xz+ivec2(80),0);
            id=int(ids[distantLayer*2+(distantSide?1:0)]);
        } else id=int(texelFetch(LocalSmooth,ivec2(materialCell.x+materialCell.z*SIDE,materialCell.y),0).r);
        vec2 corner=st;
        if(abs(normal.y)<.5)corner.y/=texelFetch(Palette,ivec2(0,value),0).w;
        if(id>0)colour=albedo*smoothLight(id,face,clamp(corner,0.0,1.0));
    }
    if(Hybrid>.5) {
        // Match vanilla's camera-relative spherical/cylindrical fog in the zero-bending limit.
        // Curved paths use endpoint distance as an appearance approximation, not optical depth.
        vec3 relative=hit-Camera;
        float fogDistance=TerrainFogRange.z>.5?max(length(relative.xz),abs(relative.y)):length(relative);
        float fogAmount=TerrainFogRange.y>TerrainFogRange.x?
                smoothstep(TerrainFogRange.x,TerrainFogRange.y,fogDistance):step(TerrainFogRange.y,fogDistance);
        colour=mix(colour,TerrainFogColour.rgb,fogAmount*TerrainFogColour.a);
    }
    return colour;
}

// Intersect one height-field column prism. Near volume is never represented here.
bool columnHit(vec3 start,vec3 delta,vec3 lower,vec3 upper,float from,float until,
               out float at,out vec3 normal) {
    at=from;float leave=until;normal=vec3(0,1,0);
    for(int a=0;a<3;a++) {
        if(abs(delta[a])<1e-10) {if(start[a]<lower[a] || start[a]>=upper[a])return false;}
        else {
            float t0=(lower[a]-start[a])/delta[a],t1=(upper[a]-start[a])/delta[a];
            if(min(t0,t1)>=at) {at=min(t0,t1);normal=vec3(0);normal[a]=-sign(delta[a]);}
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
        float until=min(leave,min(next.x,next.y)),best=2.0;int value=0,bestLayer=0;vec3 bestNormal;
        for(int layer=0;layer<2;layer++) {
            if(layer==1 && !local)break;
            vec2 sampleValue=layer==0?field.xy:field.zw;
            int id=int(sampleValue.y);if(id==0 || id==1)continue;
            float lower=local && layer==0?float(SIDE):-1024.0;
            float upper=local && layer==1?min(0.0,sampleValue.x):sampleValue.x;
            if(upper<=lower)continue;
            float at;vec3 n;
            if(columnHit(start,delta,vec3(cell.x,lower,cell.y),vec3(cell.x+1,upper,cell.y+1),t,until,at,n) && at<best) {
                best=at;value=id;bestNormal=n;bestLayer=layer;
            }
        }
        if(value!=0) {
            hit=start+best*delta;normal=bestNormal;materialCell=ivec3(cell.x,int(floor(hit.y)),cell.y);
            distantLayer=bestLayer;
            vec4 appearance=texelFetch(DistantAppearance,cell+ivec2(80),0);
            vec2 layerAppearance=bestLayer==0?appearance.xy:appearance.zw;
            float top=bestLayer==0?field.x:min(0.0,field.z);
            distantSide=hit.y<floor(top-.0001)-.00001;
            if(distantSide)value=int(layerAppearance.x);
            int lightCode=int(layerAppearance.y);surfaceLight=vec2(lightCode%16,lightCode/16);
            return value;
        }
        int axis=next.x<next.y?0:1;t=next[axis];if(t>=leave)return 0;
        cell[axis]+=stepDirection[axis];next[axis]+=dt[axis];
    }
    return 2;
}
int sceneSegment(vec3 start,vec3 end,out vec3 hit,out vec3 normal) {
    distantHit=false;surfaceLight=vec2(0,15);
    if(MeshMode>.5)return meshSegment(start,end,hit,normal);
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
void trace(vec2 uv) {
    vec2 xy=(uv*2.0-1.0)*ViewSlopes.xy;
    vec3 direction=normalize(Forward+(xy.x+ViewSlopes.z)*Right+(-xy.y+ViewSlopes.w)*Up);
    vec3 hit,normal;
    vec3 radialAxis=normalize(Camera-Source);
    float r=length(Camera-Source),mu=dot(direction,radialAxis);
    vec3 tangentVector=direction-mu*radialAxis;
    float tangent=length(tangentVector);
    if(Lensing<.5 || tangent<1e-5) {
        float distance=Hybrid>.5 && (Diagnostic<.5 || Diagnostic>2.5)?1024.0:400.0;
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
        if(MeshMode<.5 && Hybrid>.5 && Diagnostic<.5 && q.y<0.0 && Radius/q.x>max(96.0,12.0*Radius)) {
            vec3 radial=cos(phi)*radialAxis+sin(phi)*tangentAxis;
            vec3 angular=-sin(phi)*radialAxis+cos(phi)*tangentAxis;
            vec3 outgoing=normalize(-q.y*radial+q.x*angular),farHit,farNormal;
            int farValue=distantSegment(p,p+outgoing*1024.0,farHit,farNormal);
            distantHit=farValue!=0;
            fragColor=vec4(distantHit?surface(farValue,farHit,farNormal):missing(outgoing),1);return;
        }
        // Once outgoing beyond the sphere enclosing all data, no future chord can re-enter.
        if(q.y<0.0 && Radius/q.x>(MeshMode>.5?MeshExtent:Hybrid>.5 && Diagnostic<.5?512.0:max(1.5*Radius,length(max(abs(Source),abs(vec3(SIDE)-Source)))))) {
            if(MeshMode>.5) {
                vec3 radial=cos(phi)*radialAxis+sin(phi)*tangentAxis;
                vec3 angular=-sin(phi)*radialAxis+cos(phi)*tangentAxis;
                fragColor=vec4(missing(normalize(-q.y*radial+q.x*angular)),1);return;
            }
            fragColor=vec4(missing(normalize(p-Source)),1);return;
        }
        float speed=Radius*length(q)/(q.x*q.x);
        float stepSize=PathStep;
        if(Hybrid>.5 && (Diagnostic<.5 || Diagnostic>2.5)) stepSize=mix(PathStep,4.0,smoothstep(80.0,144.0,length(p-Source)));
        if(MeshMode>.5 && AdaptivePath>.5) {
            // Euclidean curvature of the embedded null orbit, u=rs/r, v=du/dphi:
            // kappa=1.5*u^5/(rs*(u*u+v*v)^(3/2)). Local chord sagitta is ~kappa*length^2/8.
            float normQ=length(q),u2=q.x*q.x;
            float curvature=1.5*u2*u2*q.x/max(Radius*normQ*normQ*normQ,1e-12);
            float tolerance=.001*(PathStep/.45)*(PathStep/.45);
            stepSize=clamp(sqrt(8.0*tolerance/max(curvature,1e-12)),PathStep,4.0);
        }
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
    if(Diagnostic>1.5 && Diagnostic<2.5) {
        vec2 xy=(screenUv*2.0-1.0)*ViewSlopes.xy;
        vec3 d=normalize(Forward+(xy.x+ViewSlopes.z)*Right+(-xy.y+ViewSlopes.w)*Up),hit,normal;
        int value=distantSegment(Camera,Camera+d*1024.0,hit,normal);
        fragColor=vec4(value==0?0.0:length(hit-Camera),0,0,float(value));return;
    }
    int samples=Diagnostic>.5?1:int(RaySamples);
    vec4 sum=vec4(0);
    for(int sampleIndex=0;sampleIndex<4;sampleIndex++) {
        if(sampleIndex>=samples)break;
        // Each ray owns its hit/cloud state; no cloud or far hit may leak into the next subpixel.
        cloudLayer=vec4(0);diagnostic=vec4(0);distantHit=false;distantLayer=0;distantSide=false;
        vec2 offset=vec2(0);
        if(samples==2)offset=vec2(sampleIndex==0?-.25:.25);
        if(samples==4)offset=vec2((sampleIndex%2)==0?-.25:.25,sampleIndex<2?-.25:.25);
        trace(screenUv+offset/Viewport);
        if(MeshMode>.5 && MeshClouds>.5)fragColor.rgb=mix(fragColor.rgb,cloudLayer.rgb,cloudLayer.a);
        sum+=fragColor;
    }
    fragColor=Diagnostic>.5?diagnostic:sum/float(samples);
}
