package io.github.rohrl.interstellar.scene;

/** Eligibility for one intersection of a native cloud face, retaining its original diagonal. */
public final class CloudQuads {
    private CloudQuads() {}
    /** Clouds do not use lightmap coordinates. Reuse four such floats for a plane
     * descriptor; positions, native UVs, colours, alpha and material flags stay exact. */
    public static void describe(float[] vertices,int offset) {
        if(!rectangle(vertices,offset))throw new IllegalArgumentException("Unsupported cloud face");
        int axis=0,first=0;
        for(int a=0;a<3;a++) {
            if(vertices[offset+a]==vertices[offset+24+a])axis=a;
            if(vertices[offset+a]!=vertices[offset+12+a])first=a;
        }
        int second=3-axis-first,x=(axis+1)%3,y=(axis+2)%3;
        float area=(vertices[offset+12+x]-vertices[offset+x])*(vertices[offset+36+y]-vertices[offset+y])
                -(vertices[offset+12+y]-vertices[offset+y])*(vertices[offset+36+x]-vertices[offset+x]);
        vertices[offset+6]=axis+first*3;vertices[offset+7]=area;
        vertices[offset+18]=1/(vertices[offset+12+first]-vertices[offset+first]);
        vertices[offset+19]=1/(vertices[offset+36+second]-vertices[offset+second]);
    }
    public static boolean rectangle(float[] vertices,int offset) {
        int first=0,second=0,constant=0;
        for(int axis=0;axis<3;axis++) {
            float a=vertices[offset+axis],b=vertices[offset+12+axis],c=vertices[offset+24+axis],d=vertices[offset+36+axis];
            if(!Float.isFinite(a) || !Float.isFinite(b) || !Float.isFinite(c) || !Float.isFinite(d))return false;
            if(a==b && a==c && a==d)constant++;
            else if(a==d && b==c)first++;
            else if(a==b && c==d)second++;
            else return false;
        }
        return first==1 && second==1 && constant==1;
    }
}
