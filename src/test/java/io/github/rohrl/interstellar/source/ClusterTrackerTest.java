package io.github.rohrl.interstellar.source;

import org.junit.jupiter.api.Test;
import java.util.*;
import static io.github.rohrl.interstellar.source.ClusterProbe.*;
import static org.junit.jupiter.api.Assertions.*;

class ClusterTrackerTest {
    private final Set<Cell> blocks=new HashSet<>();
    private final Set<Long> missing=new HashSet<>();
    private final ClusterTracker tracker=new ClusterTracker(c->missing.contains(ClusterTracker.chunk(c.x()>>4,c.z()>>4))?
            CellState.UNKNOWN:blocks.contains(c)?CellState.MASS:CellState.EMPTY,RADIUS_PER_BLOCK);
    private long tick;
    private Cell cell(int x) {return new Cell(x,8,8);}
    private void edit(int x,boolean add) {if(add)blocks.add(cell(x));else blocks.remove(cell(x));tracker.changed(cell(x),tick);}
    private void settle() {for(int i=0;i<200&&tracker.pending();i++)assertTrue(tracker.advance(++tick,13)<=13);assertFalse(tracker.pending());}
    private long complete() {return tracker.entries().stream().filter(ClusterTracker.Entry::ready).count();}
    @Test void discoversSharesAndStaysIdle() {
        for(int i=0;i<8;i++)edit(i,true);settle();assertEquals(1,complete());
        var entry=tracker.at(cell(0));assertEquals(8,entry.result.count());
        for(int i=0;i<8;i++)tracker.discover(cell(i));assertFalse(tracker.pending());
        assertEquals(0,tracker.advance(++tick,13));assertSame(entry,tracker.at(cell(7)));
    }
    @Test void anchorRemovalSplitMergeAndDelete() {
        for(int i=0;i<5;i++)edit(i,true);settle();long id=tracker.at(cell(0)).id;
        edit(0,false);settle();assertEquals(4,tracker.at(cell(1)).result.count());assertEquals(id,tracker.at(cell(1)).id);
        edit(2,false);settle();assertEquals(2,complete());assertNotEquals(tracker.at(cell(1)).id,tracker.at(cell(3)).id);
        edit(2,true);settle();assertEquals(1,complete());assertEquals(4,tracker.at(cell(1)).result.count());
        for(int i=1;i<5;i++)edit(i,false);settle();assertEquals(0,complete());assertTrue(tracker.entries().isEmpty());
    }
    @Test void unrelatedEditsDoNotInvalidateCompletedSource() {
        edit(1,true);settle();var entry=tracker.at(cell(1));edit(100,true);settle();
        assertSame(entry,tracker.at(cell(1)));assertTrue(entry.ready());assertEquals(2,complete());
    }
    @Test void partialSourceDisablesAndRecoversOnChunkLoad() {
        edit(15,true);edit(16,true);settle();assertEquals(1,complete());
        missing.add(ClusterTracker.chunk(1,0));tracker.chunkChanged(1,0,tick);assertFalse(tracker.at(cell(15)).ready());settle();
        assertEquals(0,complete());missing.clear();tracker.chunkChanged(1,0,tick);tracker.discover(cell(16));settle();
        assertEquals(1,complete());assertEquals(2,tracker.at(cell(15)).result.count());
    }
    @Test void editDuringProbeCannotPublishOldMass() {
        for(int i=0;i<60;i++)edit(i,true);tracker.advance(tick+=3,15);
        edit(1,false);settle();assertEquals(2,complete());assertEquals(1,tracker.at(cell(0)).result.count());
        assertEquals(58,tracker.at(cell(2)).result.count());
    }
}
