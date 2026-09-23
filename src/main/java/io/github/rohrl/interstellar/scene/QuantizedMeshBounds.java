package io.github.rohrl.interstellar.scene;

import java.nio.IntBuffer;

/** One integer texel for six outward-rounded 1/16-block bounds plus the escape link. */
public final class QuantizedMeshBounds {
    public static final int WIDTH=CompactMeshNodes.NODES;
    public static final int FALLBACK=0x80000000;
    private QuantizedMeshBounds() {}
    public static void write(float[] source,int length,IntBuffer destination) {
        if(length<0 || length>source.length || length%12!=0 || destination.capacity()<length/3)
            throw new IllegalArgumentException("Invalid bounds payload");
        for(int i=0;i<length/12;i++) {
            int from=i*12,to=i*4,escape=(int)source[from+3];boolean fallback=false;
            if(escape< -1 || escape>=1<<23 || escape!=source[from+3])throw new IllegalArgumentException("Invalid escape link");
            for(int a=0;a<3;a++) {
                double low=Math.floor((double)source[from+a]*16),high=Math.ceil((double)source[from+4+a]*16);
                if(!Double.isFinite(low) || !Double.isFinite(high) || low<Short.MIN_VALUE || high>Short.MAX_VALUE || low>high)fallback=true;
                destination.put(to+a,((int)low&65535)|(((int)high&65535)<<16));
            }
            // Retain the original full-precision record for out-of-range nodes.
            destination.put(to+3,(escape&0xffffff)|(fallback?FALLBACK:0));
        }
    }
}
