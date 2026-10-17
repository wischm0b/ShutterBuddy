package de.jr.pixelremote;

/** One camera action at a time, with no catch-up burst after a stalled phone. */
final class SeriesRun {
    final int interval;
    private long due, actionStarted;
    private boolean stopped, inFlight;
    private int count;
    SeriesRun(int seconds, long now) {
        if (!validInterval(seconds)) throw new IllegalArgumentException("Invalid interval");
        interval = seconds; due = now + seconds * 1000L;
    }
    static boolean validInterval(int seconds) { return seconds >= 1 && seconds <= 5; }
    long due() { return due; }
    int count() { return count; }
    boolean claim(long now) {
        if (stopped || inFlight || now < due || now > due + 2000) return false;
        inFlight = true; actionStarted = now; return true;
    }
    boolean complete(long now) {
        if (stopped || !inFlight) return false;
        inFlight = false; count++; due = now + interval * 1000L; return true;
    }
    boolean stalled(long now) { return inFlight && now - actionStarted > 5000; }
    void stop() { stopped = true; }
}
