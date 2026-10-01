package dev.nexvisuals.core.animation;

/** One accepted local swing starts a clock. Rendering only reads it; it never restarts an attack. */
public final class SwingTimeline {
    private long started = Long.MIN_VALUE;
    private long sequence = Long.MIN_VALUE;

    /** Duplicate requests in the same game tick cannot pin the cosmetic animation to frame zero. */
    public boolean start(long tick, long nowNanos) {
        if (sequence == tick) return false;
        sequence = tick;
        started = nowNanos;
        return true;
    }
    public double progress(long nowNanos, int durationMillis) {
        if (started == Long.MIN_VALUE) return 0;
        double progress = (nowNanos - started) / (Math.max(1, durationMillis) * 1_000_000.0);
        return EffectMath.unit(progress);
    }
    public long elapsedNanos(long nowNanos) { return started == Long.MIN_VALUE ? 0 : Math.max(0, nowNanos - started); }
    public void reset() { sequence = started = Long.MIN_VALUE; }
    public boolean active(long nowNanos, int durationMillis) {
        return started != Long.MIN_VALUE && nowNanos >= started
                && nowNanos-started < Math.max(1,durationMillis)*1_000_000L;
    }
}
