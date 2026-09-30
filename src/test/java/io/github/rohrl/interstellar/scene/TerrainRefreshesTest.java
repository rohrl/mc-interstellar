package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.rohrl.interstellar.scene.TerrainRefreshes.*;

class TerrainRefreshesTest {
    @Test void editJumpsAheadOfBackgroundWorkAndSkipsUnloadedEdits() {
        var work=new TerrainRefreshes();
        for(long i=0;i<600;i++)work.changed(i,CONTENT|LIGHT);
        work.changed(90,CONTENT|EDIT);work.changed(42,CONTENT|EDIT);
        assertEquals(42L,work.nextEdit(key->key!=90));
        assertTrue(work.begin(42));
        assertEquals(90L,work.nextEdit(key->true));
        assertTrue(work.begin(90));assertNull(work.nextEdit(key->true));
    }
    @Test void LightingDuringEditDoesNotDiscardGeometryButLaterEditDoes() {
        var work=new TerrainRefreshes();work.changed(7,CONTENT|EDIT);
        long captured=work.version(7);boolean edit=work.begin(7);
        for(int i=0;i<100;i++){work.changed(7,LIGHT);work.retry(7,edit);}
        assertEquals(captured,work.version(7),"Lighting must allow an active capture to finish");
        assertEquals(7L,work.nextEdit(key->true),"Latest lighting still gets a follow-up");
        work.begin(7);work.changed(7,CONTENT|EDIT);
        assertNotEquals(captured,work.version(7),"Never publish geometry captured across a block change");
        assertEquals(7L,work.nextEdit(key->true));
    }
    @Test void CoalescedEditsDoNotDuplicateAndWindowChangesDropObsoleteWork() {
        var work=new TerrainRefreshes();
        work.changed(1,CONTENT|EDIT);work.changed(1,CONTENT|EDIT|LIGHT);work.changed(2,CONTENT|EDIT);
        assertEquals(1L,work.nextEdit(key->true));work.begin(1);
        assertEquals(2L,work.nextEdit(key->true));
        work.retain(Set.of(1L));assertNull(work.nextEdit(key->true));assertEquals(0,work.version(2));
    }
    @Test void ChunkEdgesRefreshNeighboursWithoutInvalidatingWholeThreeByThree() {
        assertEquals(Set.of(pack(0,0)),Set.copyOf(affected(8,8)));
        assertEquals(Set.of(pack(0,0),pack(1,0)),Set.copyOf(affected(15,8)));
        assertEquals(Set.of(pack(-1,-1),pack(0,-1),pack(-1,0),pack(0,0)),Set.copyOf(affected(-1,-1)));
        assertEquals(Set.of(pack(-2,-1),pack(-1,-1),pack(-2,-2),pack(-1,-2)),Set.copyOf(affected(-16,-16)));
    }
    private static long pack(int x,int z){return (x&0xffffffffL)|((long)z<<32);}
}
