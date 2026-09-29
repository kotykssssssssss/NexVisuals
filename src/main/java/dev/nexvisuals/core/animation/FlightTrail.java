package dev.nexvisuals.core.animation;

/** Budget and wing-tip spacing for a pair of purely cosmetic flight trails. */
public final class FlightTrail {
    private FlightTrail() { }
    public record Point(double x, double y, double z) { }

    public static int samples(double distance, int density) {
        if (!Double.isFinite(distance) || distance < .02 || distance > 8) return 0;
        return Math.min(8, Math.max(1, (int) Math.ceil(distance * Math.clamp(density, 1, 12))));
    }

    public static Point wing(double x, double y, double z, double yawDegrees, double halfSpan, int side) {
        if (side != -1 && side != 1) throw new IllegalArgumentException("A wing is left or right");
        double radians = Math.toRadians(yawDegrees);
        return new Point(x + Math.cos(radians) * halfSpan * side, y, z + Math.sin(radians) * halfSpan * side);
    }
}
