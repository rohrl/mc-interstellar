package io.github.rohrl.interstellar.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefreshScheduleTest {
    @Test void initialRequestRunsOnceAndThenStaysIdle() {
        var schedule=new RefreshSchedule();
        assertTrue(schedule.claim(0));
        assertFalse(schedule.pending());assertFalse(schedule.claim(100000));
    }
    @Test void burstsWaitUntilFiveQuietTicks() {
        var schedule=new RefreshSchedule();schedule.claim(0);
        schedule.changed(10);schedule.changed(12);
        assertFalse(schedule.claim(15));assertFalse(schedule.claim(16));
        assertTrue(schedule.claim(17));assertFalse(schedule.claim(18));
    }
    @Test void retriesBackOffAndNewEventsCannotShortenCooldown() {
        var schedule=new RefreshSchedule();schedule.claim(0);schedule.retry(10);
        schedule.changed(11);assertFalse(schedule.claim(16));
        assertFalse(schedule.claim(29));assertTrue(schedule.claim(30));
    }
    @Test void changeDuringProbeIsNotLostAndLaterEventWakesIncompleteState() {
        var schedule=new RefreshSchedule();assertTrue(schedule.claim(0));
        schedule.changed(1);schedule.retry(2);
        assertTrue(schedule.pending());assertFalse(schedule.claim(21));assertTrue(schedule.claim(22));
        assertFalse(schedule.claim(100));
        schedule.changed(101);assertTrue(schedule.claim(106));
    }
    @Test void partialProbeAddsNewChunkDependencies() {
        var footprint=SourceFootprint.enclosing(8,8,1).including(-4,3);
        assertTrue(footprint.contains(-4,3));assertTrue(footprint.contains(0,0));
        assertFalse(footprint.contains(-5,3));
    }
}
