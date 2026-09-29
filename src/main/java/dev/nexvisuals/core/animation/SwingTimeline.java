package dev.nexvisuals.core.animation;

/** Detects a local vanilla swing cycle, but keeps cosmetic duration independent of attack cooldown. */
public final class SwingTimeline {
    private double previous;
    private long started = Long.MIN_VALUE;
    public double sample(double vanillaProgress, long nowNanos, int durationMillis) {
        double vanilla = EffectMath.unit(vanillaProgress);
        if (vanilla > 0 && (previous <= 0 || vanilla < previous - .35)) started = nowNanos;
        previous = vanilla;
        if (started == Long.MIN_VALUE) return 0;
        double progress = (nowNanos - started) / (Math.max(1, durationMillis) * 1_000_000.0);
        return progress >= 1 ? 0 : EffectMath.unit(progress);
    }
    public void reset() { previous = 0; started = Long.MIN_VALUE; }
}
