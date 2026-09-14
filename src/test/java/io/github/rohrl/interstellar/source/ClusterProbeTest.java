package io.github.rohrl.interstellar.source;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.rohrl.interstellar.source.ClusterProbe.*;

class ClusterProbeTest {
    private static Result inspect(Set<Cell> cells, Cell seed) {
        var probe = new ClusterProbe(seed,c -> cells.contains(c) ? CellState.MASS : CellState.EMPTY,4096);
        while (!probe.finished()) probe.advance(3);
        return probe.result();
    }
    @Test void singleBlockUsesItsCentreAndFarthestCorner() {
        Cell seed = new Cell(-10,300,22);
        var r=inspect(Set.of(seed),seed);
        assertEquals(1,r.count()); assertEquals(-9.5,r.x()); assertEquals(300.5,r.y()); assertEquals(22.5,r.z());
        assertEquals(Math.sqrt(3)/2,r.enclosingRadius(),1e-12);
        assertEquals(.125,r.schwarzschildRadius()); assertFalse(r.blackHoleProxy());
    }
    @Test void cubeHasExpectedMassGeometryAndCompactness() {
        var cells=new HashSet<Cell>();
        for(int x=0;x<4;x++) for(int y=0;y<4;y++) for(int z=0;z<4;z++) cells.add(new Cell(x,y,z));
        var r=inspect(cells,new Cell(0,0,0));
        assertEquals(64,r.count()); assertEquals(8,r.schwarzschildRadius());
        assertEquals(2,r.x()); assertEquals(Math.sqrt(12),r.enclosingRadius(),1e-12); assertTrue(r.blackHoleProxy());
    }
    @Test void elongatedClusterOfSameMassIsNotClassifiedAsCompact() {
        var cells=new HashSet<Cell>(); for(int x=0;x<64;x++) cells.add(new Cell(x,0,0));
        var r=inspect(cells,new Cell(0,0,0));
        assertEquals(64,r.count()); assertFalse(r.blackHoleProxy()); assertTrue(r.enclosingRadius()>32);
    }
    @Test void onlyFaceNeighboursConnectAndBridgeEditsSplitAndMerge() {
        var cells=new HashSet<>(Set.of(new Cell(0,0,0),new Cell(2,0,0),new Cell(0,1,1)));
        assertEquals(1,inspect(cells,new Cell(0,0,0)).count());
        cells.add(new Cell(1,0,0)); assertEquals(3,inspect(cells,new Cell(0,0,0)).count());
        cells.remove(new Cell(1,0,0)); assertEquals(1,inspect(cells,new Cell(0,0,0)).count());
    }
    @Test void everyAdvanceRespectsCellReadBudget() {
        var reads=new AtomicInteger();
        var p=new ClusterProbe(new Cell(0,0,0),c -> { reads.incrementAndGet(); return c.x()==0&&c.y()==0&&c.z()==0 ? CellState.MASS : CellState.EMPTY; },4096);
        while(!p.finished()) { int before=reads.get(); int used=p.advance(2); assertEquals(used,reads.get()-before); assertTrue(used<=2); }
        assertEquals(7,reads.get());
    }
    @Test void unknownCellsAndLimitNeverProduceACompleteClassification() {
        var p=new ClusterProbe(new Cell(0,0,0),c -> c.x()==1 ? CellState.UNKNOWN : c.equals(new Cell(0,0,0)) ? CellState.MASS : CellState.EMPTY,4096);
        while(!p.finished())p.advance(2);
        assertEquals(Status.PARTIAL,p.result().status()); assertFalse(p.result().blackHoleProxy());
        var huge=new ClusterProbe(new Cell(0,0,0),c -> CellState.MASS,3);
        while(!huge.finished())huge.advance(2);
        assertEquals(Status.LIMIT,huge.result().status()); assertEquals(3,huge.result().count()); assertFalse(huge.result().blackHoleProxy());
    }
    @Test void emptySeedHasNoMassAndResultCannotBeReadEarly() {
        var p=new ClusterProbe(new Cell(0,0,0),c -> CellState.EMPTY,1);
        assertThrows(IllegalStateException.class,p::result); p.advance(1);
        assertEquals(Status.EMPTY,p.result().status()); assertEquals(0,p.result().count());
    }
}
