package dev.nexvisuals.core.animation;

import java.util.function.LongSupplier;

/** A monotonic animation clock: paused motion resumes without catching up on missed frames. */
public final class MenuClock {
    private final LongSupplier clock;
    private long last;
    private double seconds;
    public MenuClock() { this(System::nanoTime); }
    public MenuClock(LongSupplier clock) { this.clock = clock; last = clock.getAsLong(); }
    public double advance(double speed, boolean moving) {
        if (!Double.isFinite(speed) || speed < 0) throw new IllegalArgumentException("Invalid menu speed");
        long now = clock.getAsLong();
        if (now < last) return seconds;
        if (moving) seconds += Math.min(.25, (now-last)/1_000_000_000.0)*speed;
        last = now;
        return seconds;
    }
}
