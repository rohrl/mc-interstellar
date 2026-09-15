package io.github.rohrl.interstellar.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceFootprintTest {
    @Test void negativeCoordinatesUseFloorAndIncludeNeighbourShell() {
        var bounds=SourceFootprint.enclosing(-8,-8,7.5);
        assertTrue(bounds.contains(-2,-2));
        assertTrue(bounds.contains(0,0));
        assertFalse(bounds.contains(-3,-1));
        assertFalse(bounds.contains(-1,1));
    }
    @Test void sourceNearChunkEdgeIncludesAdjacentConnectivityChanges() {
        var bounds=SourceFootprint.enclosing(14.5,8,.8660254);
        assertTrue(bounds.contains(1,0));
        assertFalse(bounds.contains(2,0));
        assertFalse(bounds.contains(0,1));
    }
    @Test void distantChunkActivityDoesNotAffectSavedSource() {
        var bounds=SourceFootprint.enclosing(15.976,16.008,3.5);
        assertTrue(bounds.contains(0,0));
        assertTrue(bounds.contains(1,1));
        assertFalse(bounds.contains(5,1));
        assertFalse(bounds.contains(1,-5));
    }
}
