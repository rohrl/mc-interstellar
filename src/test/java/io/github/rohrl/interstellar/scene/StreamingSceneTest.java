package io.github.rohrl.interstellar.scene;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StreamingSceneTest {
    @Test void mixedChunkReplacementAndGrowthNeverOverlapLiveAllocations() {
        var arena=new RowArena(4,32);var random=new java.util.Random(719);
        var live=new java.util.ArrayList<int[]>();
        for(int step=0;step<2000;step++) {
            if(!live.isEmpty() && random.nextBoolean()) {
                var old=live.remove(random.nextInt(live.size()));int size=1+random.nextInt(20);
                if(size<=old[1]) {
                    if(size<old[1])arena.release(old[0]+size,old[1]-size);
                    live.add(new int[]{old[0],size});
                } else if(arena.extend(old[0],old[1],size))live.add(new int[]{old[0],size});
                else arena.release(old[0],old[1]);
            } else {
                int size=1+random.nextInt(20);
                if(arena.largestFree()<size)arena.grow(arena.capacity()+32);
                live.add(new int[]{arena.allocate(size),size});
            }
            var used=new java.util.BitSet();int occupied=0;
            for(var chunk:live) {
                assertTrue(chunk[0]>=4 && chunk[0]+chunk[1]<=arena.capacity()+4);
                for(int row=chunk[0];row<chunk[0]+chunk[1];row++){assertFalse(used.get(row));used.set(row);}
                occupied+=chunk[1];
            }
            assertEquals(arena.capacity(),occupied+arena.freeRows());
        }
        for(var chunk:live)arena.release(chunk[0],chunk[1]);
        assertEquals(arena.capacity(),arena.largestFree());
    }
    @Test void growthKeepsLiveAddressesAndCoalescesTheOldTail() {
        var arena=new RowArena(4,10);assertEquals(4,arena.allocate(7));
        arena.grow(20);assertEquals(13,arena.freeRows());assertEquals(13,arena.largestFree());
        assertEquals(11,arena.allocate(13));assertEquals(0,arena.freeRows());
        arena.release(4,7);arena.release(11,13);assertEquals(4,arena.allocate(20));
        assertThrows(IllegalArgumentException.class,()->arena.grow(19));
    }
    @Test void repeatedChunkSizeChangesReuseTheirAddressWithoutFragmentation() {
        var arena=new RowArena(0,100);int edited=arena.allocate(20),neighbour=arena.allocate(80);
        assertFalse(arena.extend(edited,20,21));
        arena.release(neighbour,80);
        for(int i=0;i<1000;i++) {
            assertTrue(arena.extend(edited,20,23));assertEquals(77,arena.freeRows());
            arena.release(edited+20,3);assertEquals(80,arena.largestFree());
        }
        assertTrue(arena.extend(edited,20,100));assertEquals(0,arena.freeRows());
    }
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
