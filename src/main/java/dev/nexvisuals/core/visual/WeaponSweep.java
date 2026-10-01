package dev.nexvisuals.core.visual;

/** One bounded blade sweep. Separate attacks and discontinuous poses are never bridged. */
public final class WeaponSweep {
    private final TrailHistory history = new TrailHistory(48);
    private long sequence = Long.MIN_VALUE, sampled = Long.MIN_VALUE;
    public TrailHistory history() { return history; }
    public void start(long tick) {
        if (sequence == tick) return;
        clear(); sequence = tick;
    }
    public void clear() { history.clear(); sequence = sampled = Long.MIN_VALUE; }
    public void trim(long now, int lifetime) { history.trim(now, lifetime, 8); }
    public boolean sample(long now, double ax, double ay, double az, double bx, double by, double bz) {
        if (sampled != Long.MIN_VALUE && now >= sampled && now - sampled < 10) return false;
        if (sampled != Long.MIN_VALUE && (now < sampled || now - sampled > 100)) history.clear();
        // A probe at/behind the camera can produce a huge clipped sheet across the whole screen.
        if (az >= -.04 || bz >= -.04) { history.clear(); sampled = now; return false; }
        boolean added = history.edges(now, ax, ay, az, bx, by, bz, .002, .8);
        sampled = now;
        return added;
    }
}
