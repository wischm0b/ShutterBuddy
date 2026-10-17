package de.jr.pixelremote;

import org.junit.Test;
import static org.junit.Assert.*;

public class SeriesRunTest {
    @Test public void intervalHasSeparateOneToFiveSecondLimits() {
        assertFalse(SeriesRun.validInterval(0)); assertFalse(SeriesRun.validInterval(6));
        for (int i = 1; i <= 5; i++) assertTrue(SeriesRun.validInterval(i));
    }
    @Test public void firstPhotoWaitsAndActionsCannotOverlap() {
        SeriesRun run = new SeriesRun(2, 100);
        assertFalse(run.claim(2099)); assertTrue(run.claim(2100)); assertFalse(run.claim(4100));
        assertTrue(run.complete(2500)); assertEquals(1, run.count());
        assertFalse(run.claim(4499)); assertTrue(run.claim(4500));
    }
    @Test public void lateAndStalledActionsAreNotRetried() {
        SeriesRun run = new SeriesRun(1, 0);
        assertFalse(run.claim(3001));
        SeriesRun waiting = new SeriesRun(1, 0);
        assertTrue(waiting.claim(1000)); assertFalse(waiting.stalled(6000)); assertTrue(waiting.stalled(6001));
    }
    @Test public void stoppingRejectsAnAlreadyDispatchedCompletion() {
        SeriesRun run = new SeriesRun(1, 0);
        assertTrue(run.claim(1000)); run.stop();
        assertFalse(run.complete(1001)); assertFalse(run.claim(2000)); assertEquals(0, run.count());
    }
}
