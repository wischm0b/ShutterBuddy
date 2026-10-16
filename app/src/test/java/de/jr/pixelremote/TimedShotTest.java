package de.jr.pixelremote;
import org.junit.Test;
import static org.junit.Assert.*;
public class TimedShotTest {
    @Test public void waitsForDeadlineAndExecutesOnlyOnce() {
        TimedShot shot = new TimedShot(3, 100);
        assertFalse(shot.claim(3099)); assertTrue(shot.claim(3100)); assertFalse(shot.claim(3101));
    }
    @Test public void cancelledCountdownCannotFire() {
        TimedShot shot = new TimedShot(10, 0);
        assertTrue(shot.cancel()); assertFalse(shot.claim(10000));
    }
    @Test public void stalledMainThreadDoesNotCauseMuchLaterPhoto() {
        assertFalse(new TimedShot(5, 0).claim(7001));
    }
    @Test public void invalidDelaysAreRejected() {
        assertFalse(TimedShot.validDelay(-1)); assertFalse(TimedShot.validDelay(2)); assertFalse(TimedShot.validDelay(60));
        assertTrue(TimedShot.validDelay(0)); assertTrue(TimedShot.validDelay(10));
    }
}
