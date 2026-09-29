package dev.nexvisuals.core.animation;

/** Pure bounded math shared by particles, swing and interface transitions. */
public final class EffectMath {
    private EffectMath() { }
    public static double unit(double value) { return Double.isFinite(value) ? Math.clamp(value, 0, 1) : 0; }
    public static double envelope(double progress, double peak, Easing easing) {
        double p = unit(progress), split = Math.clamp(peak, .1, .9);
        return p <= split ? easing.apply(p / split) : 1 - easing.apply((p - split) / (1 - split));
    }
    public static int color(int a, int b, double progress) {
        double t = unit(progress); int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int x = (a >>> shift) & 255, y = (b >>> shift) & 255;
            out |= ((int) Math.round(x + (y - x) * t)) << shift;
        }
        return out;
    }
    public static double arc(double progress) { double p = unit(progress); return 4 * p * (1 - p); }
}
