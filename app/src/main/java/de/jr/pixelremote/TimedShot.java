package de.jr.pixelremote;

/** A cancelled or stalled countdown must never turn into an unexpected later photo. */
final class TimedShot {
    private final long due;
    private boolean cancelled, claimed;
    TimedShot(int seconds, long now) {
        if (!validDelay(seconds)) throw new IllegalArgumentException("Unsupported countdown");
        due = now + seconds * 1000L;
    }
    static boolean validDelay(int seconds) { return seconds == 0 || seconds == 3 || seconds == 5 || seconds == 10; }
    boolean claim(long now) {
        if (cancelled || claimed || now < due) return false;
        if (now > due + 2000) { cancelled = true; return false; }
        claimed = true; return true;
    }
    boolean cancel() {
        if (claimed) return false;
        cancelled = true; return true;
    }
}
