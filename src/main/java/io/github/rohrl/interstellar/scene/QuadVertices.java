package io.github.rohrl.interstellar.scene;

/** Lossless storage for native (0,1,2), (2,3,0) triangle pairs. No planar assumption. */
public final class QuadVertices {
    public static final int STRIDE=48,WIDTH=4092,FLOATS=WIDTH*4,PER_ROW=FLOATS/STRIDE;
    private QuadVertices() {}
    public static float[] pack(float[] triangles,int count) {
        if(count<0 || count%2!=0 || count>triangles.length/36)throw new IllegalArgumentException("Expected complete native triangle pairs");
        var result=new float[count/2*STRIDE];
        for(int q=0;q<count/2;q++) {
            int from=q*72,to=q*STRIDE;
            for(int i=0;i<12;i++)if(Float.floatToRawIntBits(triangles[from+i])!=Float.floatToRawIntBits(triangles[from+60+i])
                    || Float.floatToRawIntBits(triangles[from+24+i])!=Float.floatToRawIntBits(triangles[from+36+i]))
                throw new IllegalArgumentException("Native pair lost shared vertices before packing");
            System.arraycopy(triangles,from,result,to,36);
            System.arraycopy(triangles,from+48,result,to+36,12);
        }
        return result;
    }
    /** Existing independent triangle fixtures: alternate which half is degenerate.
     * Each active half keeps its original vertex order and complete attributes. */
    public static float[] fixture(float[] triangles,int length) {
        if(length<0 || length>triangles.length || length%36!=0)throw new IllegalArgumentException("Invalid fixture");
        var result=new float[length/36*STRIDE];
        for(int i=0;i<length/36;i++) {
            int from=i*36,to=i*STRIDE;
            if((i&1)==0) {System.arraycopy(triangles,from,result,to,36);System.arraycopy(triangles,from+24,result,to+36,12);}
            else {System.arraycopy(triangles,from+24,result,to,12);System.arraycopy(triangles,from+24,result,to+12,12);System.arraycopy(triangles,from,result,to+24,24);}
        }
        return result;
    }
}
