package dev.nexvisuals.core.visual;

/** Fixed-size history of a point or a pair of ribbon edges. Times are monotonic milliseconds. */
public final class TrailHistory {
    private final double[] positions, distances;
    private final long[] times;
    private int first, size;
    public TrailHistory(int capacity) {
        if (capacity < 2 || capacity > 256) throw new IllegalArgumentException("Capacity must be 2..256");
        positions = new double[capacity * 6]; distances = new double[capacity]; times = new long[capacity];
    }
    private int index(int point) { return (first + point) % times.length; }
    public int size() { return size; }
    public double coordinate(int point, int axis) { return positions[index(point) * 6 + axis]; }
    public long time(int point) { return times[index(point)]; }
    public double distance(int point) { return distances[index(point)]; }
    public void clear() { first = size = 0; }
    public boolean point(long time, double x, double y, double z, double minimum, double breakDistance) {
        return edges(time, x, y, z, x, y, z, minimum, breakDistance);
    }
    public boolean edges(long time, double ax, double ay, double az, double bx, double by, double bz,
                         double minimum, double breakDistance) {
        if (!Double.isFinite(minimum) || !Double.isFinite(breakDistance) || minimum < 0 || breakDistance <= 0)
            throw new IllegalArgumentException("Invalid trail sampling distances");
        if (!Double.isFinite(ax) || !Double.isFinite(ay) || !Double.isFinite(az)
                || !Double.isFinite(bx) || !Double.isFinite(by) || !Double.isFinite(bz)) { clear(); return false; }
        double travelled = 0, cumulative = 0;
        if (size > 0) {
            int previous = size - 1;
            double a = length(ax-coordinate(previous,0), ay-coordinate(previous,1), az-coordinate(previous,2));
            double b = length(bx-coordinate(previous,3), by-coordinate(previous,4), bz-coordinate(previous,5));
            travelled = Math.max(a,b);
            if (time < time(previous) || travelled > breakDistance) clear();
            else {
                if (travelled < minimum) return false;
                cumulative = distance(previous);
            }
        }
        if (size == times.length) { first = (first + 1) % times.length; size--; }
        int slot = index(size++), offset = slot * 6;
        positions[offset]=ax; positions[offset+1]=ay; positions[offset+2]=az;
        positions[offset+3]=bx; positions[offset+4]=by; positions[offset+5]=bz;
        times[slot]=time; distances[slot]=cumulative + (size == 1 ? 0 : travelled);
        return true;
    }
    public void trim(long now, long lifetime, double maximumLength) {
        if (lifetime <= 0 || !Double.isFinite(maximumLength) || maximumLength <= 0)
            throw new IllegalArgumentException("Invalid trail lifetime or length");
        if (size == 0) return;
        if (now < time(size-1)) { clear(); return; }
        double newestDistance = distance(size-1);
        while (size > 0 && (now-time(0) > lifetime || newestDistance-distance(0) > maximumLength)) {
            first = (first+1) % times.length; size--;
        }
    }
    private static double length(double x,double y,double z) { return Math.sqrt(x*x+y*y+z*z); }
}
