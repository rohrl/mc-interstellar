package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class TriangleFacingTest {
    @Test void hintsOnlyRejectTrianglesWhoseOriginalDeterminantFails() {
        var random=new Random(5813);
        for(int axis=0;axis<3;axis++)for(int sign:new int[]{-1,1}) {
            int u=(axis+1)%3,v=(axis+2)%3;
            float[] data=new float[36];
            for(int vertex=0;vertex<3;vertex++)for(int a=0;a<3;a++)data[vertex*12+a]=200+a*.25f;
            data[12+u]+=2;data[24+u]+=2;data[24+v]+=sign*3;
            float[] original=data.clone();TriangleFacing.tag(data,0);
            assertEquals(axis*2+(sign>0?1:2),(int)data[15]);
            for(int i=0;i<36;i++)if(i!=15)assertEquals(original[i],data[i]);
            for(int sample=0;sample<1000;sample++) {
                float[] delta={random.nextFloat()*32-16,random.nextFloat()*32-16,random.nextFloat()*32-16};
                if(sample%5==0)delta[axis]=0;
                if(delta[axis]*sign<0)continue;
                float[] e1=new float[3],e2=new float[3],p=new float[3];
                for(int a=0;a<3;a++){e1[a]=data[12+a]-data[a];e2[a]=data[24+a]-data[a];}
                for(int a=0;a<3;a++){int b=(a+1)%3,c=(a+2)%3;p[a]=delta[b]*e2[c]-delta[c]*e2[b];}
                float det=e1[0]*p[0]+e1[1]*p[1]+e1[2]*p[2];
                assertTrue(det<1e-10f,"Hint may only reject an already rejected back/parallel face");
            }
        }
    }
    @Test void preservesEntitiesAndAvoidsAmbiguousOrDegenerateFaces() {
        float[] oblique=new float[36];oblique[12]=1;oblique[13]=2;oblique[24]=2;oblique[25]=1;
        TriangleFacing.tag(oblique,0);assertEquals(0,oblique[15]);
        float[] degenerate=new float[36];degenerate[12]=1;degenerate[24]=2;
        TriangleFacing.tag(degenerate,0);assertEquals(0,degenerate[15]);
        for(float flag:new float[]{-2,-1,1,2,5,6}) {
            float[] entity=oblique.clone();entity[3]=flag;entity[15]=flag;float[] before=entity.clone();
            TriangleFacing.tag(entity,0);assertArrayEquals(before,entity);
        }
    }
}
