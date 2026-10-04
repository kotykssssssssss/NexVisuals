package dev.nexvisuals.core.visual;

import dev.nexvisuals.core.animation.FlightTrail;

/** Samples only the last observed segment. Hidden intervals and teleports never form bridges. */
public final class MotionTrail {
    private boolean primed;
    private double previousX, previousY, previousZ;
    private double fromX, fromY, fromZ, toX, toY, toZ;

    public int observe(double x, double y, double z, int density, int budget) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            clear();
            return 0;
        }
        fromX = previousX; fromY = previousY; fromZ = previousZ;
        toX = x; toY = y; toZ = z;
        double dx = x - previousX, dy = y - previousY, dz = z - previousZ;
        int count = primed ? Math.min(Math.max(0, budget), FlightTrail.samples(Math.sqrt(dx * dx + dy * dy + dz * dz), density)) : 0;
        previousX = x; previousY = y; previousZ = z;
        primed = true;
        return count;
    }

    public double x(double progress) { return fromX + (toX - fromX) * Math.clamp(progress, 0, 1); }
    public double y(double progress) { return fromY + (toY - fromY) * Math.clamp(progress, 0, 1); }
    public double z(double progress) { return fromZ + (toZ - fromZ) * Math.clamp(progress, 0, 1); }
    public void clear() { primed = false; }
}
