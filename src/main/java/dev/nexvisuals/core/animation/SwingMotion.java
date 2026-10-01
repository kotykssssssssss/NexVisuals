package dev.nexvisuals.core.animation;

/** Bounded first-person poses, shared by the renderer and deterministic tests. */
public final class SwingMotion {
    public enum Style { VANILLA, SMOOTH, SWIPE, SLASH, SPIN, PUSH, CUSTOM }

    /** Reused per hand: no pose objects are allocated in the render loop. */
    public static final class Pose {
        public double x, y, z, pitch, yaw, roll, scale = 1;
        public void identity() { x = y = z = pitch = yaw = roll = 0; scale = 1; }
        public void copy(Pose source) {
            x = source.x; y = source.y; z = source.z; scale = source.scale;
            pitch = wrap(source.pitch); yaw = wrap(source.yaw); roll = wrap(source.roll);
        }
        public void carry(Pose source, double weight) {
            double w = EffectMath.unit(weight);
            x += source.x * w; y += source.y * w; z += source.z * w;
            pitch += source.pitch * w; yaw += source.yaw * w; roll += source.roll * w;
            scale += (source.scale - 1) * w;
        }
        private static double wrap(double degrees) { return degrees - 360 * Math.floor((degrees + 180) / 360); }
    }

    private SwingMotion() { }
    public static void sample(Style style, double progress, double peak, Easing easing, double amplitude,
                              double x, double y, double z, double pitch, double yaw, double roll,
                              double scale, Pose output) {
        output.identity();
        double p = EffectMath.unit(progress);
        if (p <= 0 || p >= 1 || style == Style.VANILLA) return;
        double strength = Math.clamp(amplitude, 0, 2);
        if (strength == 0) return;
        double a = EffectMath.envelope(p, peak, easing) * strength;
        // Different paths, not merely different rotations of the same static peak pose.
        double arc = Math.sin(Math.PI * p) * strength;
        switch (style) {
            case SMOOTH -> { output.y = .10 * a; output.z = -.16 * a; output.pitch = -38 * a; output.yaw = 18 * a; output.roll = -28 * a; }
            case SWIPE -> { output.x = -.48 * a; output.y = .10 * arc * (1 - 2 * p); output.yaw = 60 * a; output.roll = -65 * a; }
            case SLASH -> { output.x = -.30 * a; output.y = -.25 * a; output.z = -.12 * arc; output.pitch = -75 * a; output.roll = -100 * a; }
            case SPIN -> { output.y = .12 * arc; output.z = -.08 * arc; output.roll = 360 * easing.apply(p) * Math.min(1, strength); }
            case PUSH -> { output.z = -.58 * a; output.pitch = -15 * a; output.scale = 1 + .08 * a; }
            case CUSTOM -> { output.x = x * a; output.y = y * a; output.z = z * a; output.pitch = pitch * a; output.yaw = yaw * a; output.roll = roll * a; output.scale = Math.clamp(1 + (scale - 1) * a, .1, 3); }
            default -> { }
        }
        // A reduced spin amplitude returns through neutral rather than snapping from a partial turn.
        if (style == Style.SPIN && strength < 1) output.roll = 360 * a;
    }
    public static double restartCarry(long elapsedNanos) {
        return 1 - Easing.OUT_CUBIC.apply(elapsedNanos / 65_000_000.0);
    }
}
