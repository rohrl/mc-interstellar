package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.nio.FloatBuffer;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CompactMeshNodesTest {
    @Test void roundTripsBoundsLinksAndAllLeafKindsAcrossRows() {
        int count=CompactMeshNodes.NODES+17,firstRow=73;
        float[] nodes=new float[count*12];var random=new Random(1729);
        for(int i=0;i<count;i++) {
            int p=i*12;
            for(int c=0;c<7;c++)nodes[p+c]=random.nextFloat()*2048-1024;
            nodes[p+3]=i%2==0?-1:5_000_000+i;
            nodes[p+7]=i%7==0?(1<<23)-1:random.nextInt(7_000_000);
            nodes[p+8]=switch(i%12){case 0->-1;case 1->0;case 10->73;case 11->7_000_000;default->i%12-1;};
        }
        var packed=FloatBuffer.allocate(CompactMeshNodes.rows(nodes.length)*CompactMeshNodes.FLOATS);
        CompactMeshNodes.write(nodes,nodes.length,firstRow,packed);
        for(int i=0;i<count;i++) {
            int row=i/CompactMeshNodes.NODES,slot=i%CompactMeshNodes.NODES,base=row*CompactMeshNodes.FLOATS+slot*8;
            for(int c=0;c<7;c++)assertEquals(Float.floatToRawIntBits(nodes[i*12+c]),Float.floatToRawIntBits(packed.get(base+c)));
            float carrier=packed.get(base+7);assertTrue(Float.isFinite(carrier));assertTrue(carrier>=2);
            int header=Float.floatToRawIntBits(carrier)&0x07ffffff,tag=header&15,pointer=header>>>4,leafCount=tag;
            if(tag==9)leafCount=-1;
            else if(tag==15) {
                assertEquals(firstRow*CompactMeshNodes.NODES+i,pointer);
                int local=pointer-firstRow*CompactMeshNodes.NODES;
                int extra=local/CompactMeshNodes.NODES*CompactMeshNodes.FLOATS+(2730+local%CompactMeshNodes.NODES)*4;
                pointer=(int)packed.get(extra);leafCount=(int)packed.get(extra+1);
            }
            assertEquals((int)nodes[i*12+8],leafCount);
            if(leafCount!=0)assertEquals((int)nodes[i*12+7],pointer);
        }
    }
    @Test void rejectsUnrepresentablePointersAndIncompleteRecords() {
        float[] node=new float[12];node[7]=1<<23;node[8]=1;
        var buffer=FloatBuffer.allocate(CompactMeshNodes.FLOATS);
        assertThrows(IllegalArgumentException.class,()->CompactMeshNodes.write(node,12,0,buffer));
        assertThrows(IllegalArgumentException.class,()->CompactMeshNodes.write(node,11,0,buffer));
    }
}
