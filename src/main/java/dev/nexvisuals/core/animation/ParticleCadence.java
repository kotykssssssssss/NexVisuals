package dev.nexvisuals.core.animation;

/** Spreads a desired steady particle population over ticks without bursts or accumulated backlog. */
public final class ParticleCadence {
    private double remainder;
    public int next(int population, int lifetime, int maximumPerTick) {
        if (population < 0 || lifetime <= 0 || maximumPerTick <= 0) throw new IllegalArgumentException("Invalid particle cadence");
        double due = remainder + (double) population / lifetime;
        int count = (int) Math.min(maximumPerTick, Math.floor(due));
        remainder = due - Math.floor(due);
        return count;
    }
    public void reset() { remainder = 0; }
}
