package dev.nexvisuals.core.visual;

import java.util.Arrays;

/** Air-only vanilla 1.21.11 integration. Block/fluid/chunk queries belong to the client adapter. */
public final class TrajectoryMath {
    public static final int MAX_STEPS = 120;
    public static final double MAX_DISTANCE = 160;
    public enum Physics {
        ARROW(.05, false), THROWABLE(.03, true), POTION(.05, true);
        public final double gravity;
        public final boolean forcesBeforeMove;
        Physics(double gravity, boolean before) { this.gravity = gravity; forcesBeforeMove = before; }
    }
    public enum End { TIME_LIMIT, DISTANCE_LIMIT, BLOCK, UNCERTAIN }
    public record Launch(double x, double y, double z, double vx, double vy, double vz) { }
    public enum Contact { CLEAR, BLOCK, UNCERTAIN }
    /** Reused for each trace, so the adapter need not allocate a result for every simulated tick. */
    public static final class Trace {
        public Contact contact = Contact.CLEAR;
        public double x, y, z;
        public int nx, ny, nz;
        public void block(double x, double y, double z, int nx, int ny, int nz) {
            contact = Contact.BLOCK; this.x = x; this.y = y; this.z = z;
            this.nx = nx; this.ny = ny; this.nz = nz;
        }
    }
    @FunctionalInterface public interface Collision {
        void trace(double x, double y, double z, double nextX, double nextY, double nextZ, Trace result);
    }
    public static final class Path {
        public static final Path EMPTY = new Path(new double[0], End.UNCERTAIN, 0, 0, 0);
        private final double[] coordinates;
        private final End end;
        private final int nx, ny, nz;
        private Path(double[] coordinates, End end, int nx, int ny, int nz) {
            this.coordinates = coordinates; this.end = end; this.nx = nx; this.ny = ny; this.nz = nz;
        }
        public int points() { return coordinates.length / 3; }
        public double coordinate(int point, int axis) { return coordinates[point * 3 + axis]; }
        public End end() { return end; }
        public int normal(int axis) { return axis == 0 ? nx : axis == 1 ? ny : nz; }
    }
    private final double[] scratch = new double[(MAX_STEPS + 1) * 3];
    private final Trace trace = new Trace();

    public static float bowPower(int ticks) {
        float f = Math.max(0, ticks) / 20f;
        return Math.min(1, (f * f + 2 * f) / 3);
    }

    /** Inputs use Minecraft's own float sin/cos; random server spread deliberately is not guessed. */
    public static Launch launch(double x, double y, double z, double dx, double dy, double dz,
                                double speed, double inheritedX, double inheritedY, double inheritedZ) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!Double.isFinite(length) || length < 1e-9 || !Double.isFinite(speed) || speed <= 0) return null;
        return new Launch(x, y, z, dx / length * speed + inheritedX,
                dy / length * speed + inheritedY, dz / length * speed + inheritedZ);
    }

    public Path predict(Launch launch, Physics physics, int ticks, double distance, Collision collision) {
        if (launch == null || physics == null || !finite(launch) || !Double.isFinite(distance) || distance <= 0 || ticks <= 0) return Path.EMPTY;
        double x = launch.x(), y = launch.y(), z = launch.z();
        double vx = launch.vx(), vy = launch.vy(), vz = launch.vz();
        double travelled = 0, limit = Math.min(MAX_DISTANCE, distance);
        int count = 1;
        put(0, x, y, z);
        End end = End.TIME_LIMIT;
        for (int step = 0; step < Math.min(MAX_STEPS, ticks); step++) {
            // 1.21.11 throwables apply gravity and drag BEFORE moving; arrows apply both AFTER.
            if (physics.forcesBeforeMove) { vx *= .99F; vy = (vy - physics.gravity) * .99F; vz *= .99F; }
            double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (!Double.isFinite(length) || length > 8 || length < 1e-9) { end = End.UNCERTAIN; break; }
            double fraction = Math.min(1, (limit - travelled) / length);
            double nextX = x + vx * fraction, nextY = y + vy * fraction, nextZ = z + vz * fraction;
            trace.contact = Contact.CLEAR; trace.nx = trace.ny = trace.nz = 0;
            collision.trace(x, y, z, nextX, nextY, nextZ, trace);
            if (trace.contact == Contact.UNCERTAIN) { end = End.UNCERTAIN; break; }
            if (trace.contact == Contact.BLOCK) {
                if (!Double.isFinite(trace.x) || !Double.isFinite(trace.y) || !Double.isFinite(trace.z)) { end = End.UNCERTAIN; break; }
                put(count++, trace.x, trace.y, trace.z); end = End.BLOCK; break;
            }
            put(count++, nextX, nextY, nextZ);
            travelled += length * fraction;
            x = nextX; y = nextY; z = nextZ;
            if (fraction < 1 || travelled >= limit - 1e-9) { end = End.DISTANCE_LIMIT; break; }
            if (!physics.forcesBeforeMove) { vx *= .99F; vy = vy * .99F - physics.gravity; vz *= .99F; }
        }
        return new Path(Arrays.copyOf(scratch, count * 3), end,
                end == End.BLOCK ? trace.nx : 0, end == End.BLOCK ? trace.ny : 0, end == End.BLOCK ? trace.nz : 0);
    }
    private void put(int point, double x, double y, double z) {
        scratch[point * 3] = x; scratch[point * 3 + 1] = y; scratch[point * 3 + 2] = z;
    }
    private static boolean finite(Launch l) {
        return Double.isFinite(l.x()) && Double.isFinite(l.y()) && Double.isFinite(l.z())
                && Double.isFinite(l.vx()) && Double.isFinite(l.vy()) && Double.isFinite(l.vz());
    }
}
