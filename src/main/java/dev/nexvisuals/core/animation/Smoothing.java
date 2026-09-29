package dev.nexvisuals.core.animation;

/** Exponential response gives the same result when a time interval is split into many frames. */
public final class Smoothing {
    private Smoothing() { }
    public static double approach(double current, double target, double elapsedMillis, double responseMillis) {
        if (!Double.isFinite(current) || !Double.isFinite(target) || !Double.isFinite(elapsedMillis)
                || !Double.isFinite(responseMillis) || elapsedMillis < 0 || responseMillis < 0) {
            throw new IllegalArgumentException("Smoothing needs finite values and nonnegative times");
        }
        if (responseMillis == 0) return target;
        return current + (target - current) * -Math.expm1(-elapsedMillis / responseMillis);
    }
}
