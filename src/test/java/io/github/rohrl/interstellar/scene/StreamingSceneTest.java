package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StreamingSceneTest {
    @Test void fragmentedFailurePreservesFreeRangesAndRejectsInvalidRelease() {
        var arena=new RowArena(0,12);arena.allocate(4);arena.allocate(4);arena.allocate(4);
        arena.release(0,4);arena.release(8,4);
        assertThrows(IllegalStateException.class,()->arena.allocate(8));
        assertThrows(IllegalArgumentException.class,()->arena.release(0,4));
        assertThrows(IllegalArgumentException.class,()->arena.release(12,1));
        arena.release(4,4);assertEquals(0,arena.allocate(12));
    }
    @Test void arenaReusesAndCoalescesWithoutOverlappingLiveData() {
        var arena=new RowArena(4,16);
        int a=arena.allocate(4),b=arena.allocate(4),c=arena.allocate(8);
        assertEquals(4,a);assertEquals(8,b);assertEquals(12,c);
        assertThrows(IllegalStateException.class,()->arena.allocate(1));
        arena.release(a,4);arena.release(b,4);assertEquals(4,arena.allocate(8));
        arena.release(c,8);assertEquals(12,arena.allocate(8));
    }
    @Test void sceneLeavesKeepExternalPointersAndPreorderEscapes() {
        var tree=new SceneTree(List.of(new SceneTree.Part(-16,-64,-32,0,320,-16,6000),new SceneTree.Part(16,0,0,32,64,16,8000)));
        var n=tree.nodes();assertEquals(36,n.length);assertEquals(-16,n[0]);assertEquals(32,n[4]);assertEquals(3,n[3]);
        assertEquals(-1,n[20]);assertEquals(2,n[15]);
        assertEquals(-1,n[32]);assertEquals(3,n[27]);
        assertEquals(java.util.Set.of(6000f,8000f),java.util.Set.of(n[19],n[31]));
        assertEquals(0,new SceneTree(List.of()).nodes().length);
    }
}
