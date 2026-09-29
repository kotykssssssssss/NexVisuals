package dev.nexvisuals.core.animation;

/** Camera angles for the existing menu cubemap, never for the player's camera. */
public final class MenuMotion {
    public enum Style { STILL, ORBIT, SWAY, DRIFT }
    public record Angles(float pitch, float yaw) { }
    private MenuMotion() { }

    public static Angles angles(Style style, double seconds, double amplitude, double pitch, double yaw, boolean reverse) {
        if (!Double.isFinite(seconds) || !Double.isFinite(amplitude) || !Double.isFinite(pitch) || !Double.isFinite(yaw))
            throw new IllegalArgumentException("Menu motion must be finite");
        double t = reverse ? -seconds : seconds;
        double rotation = switch (style) {
            case STILL -> 0;
            case ORBIT, DRIFT -> t * 2;
            case SWAY -> Math.sin(t * .18) * amplitude;
        };
        double tilt = style == Style.DRIFT ? Math.sin(t * .12) * amplitude * .2 : 0;
        double wrapped = ((yaw + rotation) % 360 + 540) % 360 - 180;
        return new Angles((float) Math.clamp(pitch + tilt, -40, 40), (float) wrapped);
    }

    /** A staggered slide reaches a stable, clickable resting position. */
    public static double entrance(double milliseconds, int row, int duration) {
        if (duration == 0) return 1;
        return Easing.OUT_CUBIC.apply(Math.clamp((milliseconds - row * 35.0) / duration, 0, 1));
    }
}
