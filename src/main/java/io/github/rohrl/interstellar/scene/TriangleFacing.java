package io.github.rohrl.interstellar.scene;

/** Conservative direction hint in vertex B's otherwise unused flag slot. Vertex A keeps its
 * original material/entity flag, so existing shaders read the exact original geometry. */
public final class TriangleFacing {
    private TriangleFacing() { }
    public static void tag(float[] data,int at) {
        if(data[at+3]!=0)return; // Terrain only; entity and cloud flags remain untouched.
        data[at+15]=0;
        for(int axis=0;axis<3;axis++) {
            if(data[at+axis]!=data[at+12+axis] || data[at+axis]!=data[at+24+axis])continue;
            int u=(axis+1)%3,v=(axis+2)%3;
            float bu=data[at+12+u]-data[at+u],bv=data[at+12+v]-data[at+v];
            float cu=data[at+24+u]-data[at+u],cv=data[at+24+v]-data[at+v];
            // Require a zero projected edge component: the determinant has no subtractive cancellation.
            if(bu!=0 && bv!=0 && cu!=0 && cv!=0)continue;
            float area=bu*cv-bv*cu;
            if(!Float.isFinite(area) || Math.abs(area)<1e-10f)continue;
            data[at+15]=axis*2+(area>0?1:2);return;
        }
    }
}
