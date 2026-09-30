package io.github.rohrl.interstellar.scene;

import java.util.*;
import java.util.function.LongPredicate;

/** Render-thread scheduling: visible edits precede streaming, lighting never invalidates geometry. */
public final class TerrainRefreshes {
    public static final int LIGHT=1,CONTENT=2,EDIT=4;
    private final Map<Long,Long> versions=new HashMap<>();
    private final LinkedHashSet<Long> urgent=new LinkedHashSet<>();
    public void changed(long key,int kind) {
        if((kind&CONTENT)!=0)versions.merge(key,1L,Long::sum);
        if((kind&EDIT)!=0)urgent.add(key);
    }
    public long version(long key){return versions.getOrDefault(key,0L);}
    public Long nextEdit(LongPredicate loaded) {for(long key:urgent)if(loaded.test(key))return key;return null;}
    public boolean begin(long key){return urgent.remove(key);}
    public void retry(long key,boolean edit){if(edit)urgent.add(key);}
    public void retain(Set<Long> wanted){versions.keySet().retainAll(wanted);urgent.retainAll(wanted);}
    public void remove(long key){urgent.remove(key);}
    public void clear(){versions.clear();urgent.clear();}
    /** Face culling/AO can also change across a chunk edge, including diagonal corners. */
    public static List<Long> affected(int x,int z) {
        var result=new ArrayList<Long>(4);long centre=pack(x>>4,z>>4);result.add(centre);
        for(int cx=(x-1)>>4;cx<=(x+1)>>4;cx++)for(int cz=(z-1)>>4;cz<=(z+1)>>4;cz++) {
            long key=pack(cx,cz);if(key!=centre)result.add(key);
        }
        return result;
    }
    private static long pack(int x,int z){return (x&0xffffffffL)|((long)z<<32);}
}
