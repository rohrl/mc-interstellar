package io.github.rohrl.interstellar.scene;

import java.nio.FloatBuffer;

/** Exact two-texel node records. Preserve virtual node addresses and reserve each row's final
 * third for unusually large leaf descriptors. The header is a normal finite float bit carrier. */
public final class CompactMeshNodes {
    public static final int WIDTH=4095,NODES=1365,FLOATS=WIDTH*4;
    private CompactMeshNodes() { }
    public static int rows(int length) {return Math.max(1,(length/12+NODES-1)/NODES);}
    public static void write(float[] source,int length,int firstRow,FloatBuffer destination) {
        if(length<0 || length>source.length || length%12!=0 || firstRow<0)throw new IllegalArgumentException("Invalid node payload");
        for(int i=0;i<length/12;i++) {
            int from=i*12,row=i/NODES,slot=i%NODES,to=row*FLOATS+slot*8;
            for(int c=0;c<7;c++)destination.put(to+c,source[from+c]);
            int count=(int)source[from+8],pointer=(int)source[from+7],tag=count<0?9:count;
            if(count==0)pointer=0;
            else if(count>8) {
                tag=15;pointer=firstRow*NODES+i;
                int extra=row*FLOATS+(NODES*2+slot)*4;
                destination.put(extra,source[from+7]);destination.put(extra+1,source[from+8]);
            }
            if(pointer<0 || pointer>=1<<23)throw new IllegalArgumentException("Compact node pointer out of range");
            // Bits 30 and 27..29 keep the exponent normal, finite, and independent of the payload.
            destination.put(to+7,Float.intBitsToFloat(0x40000000|(pointer<<4)|tag));
        }
    }
}
