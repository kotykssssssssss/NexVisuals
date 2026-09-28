package dev.nexvisuals.core.animation;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Time-based reversible progress. Call target every frame without restarting the transition. */
public final class Transition {
    private final LongSupplier nanoClock;
    private final Easing easing;
    private long durationNanos;
    private long lastTick;
    private double progress;
    private boolean forward;

    public Transition(long durationMillis, Easing easing) {
        this(durationMillis, easing, System::nanoTime);
    }

    public Transition(long durationMillis, Easing easing, LongSupplier nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock);
        this.easing = Objects.requireNonNull(easing);
        this.lastTick = nanoClock.getAsLong();
        this.durationNanos = durationNanos(durationMillis);
    }

    public void target(boolean forward) {
        update();
        this.forward = forward;
        if (durationNanos == 0) progress = forward ? 1 : 0;
    }

    public void setDurationMillis(long durationMillis) {
        long newDuration = durationNanos(durationMillis);
        update();
        durationNanos = newDuration;
        if (newDuration == 0) progress = forward ? 1 : 0;
    }

    public void snap(boolean forward) {
        this.forward = forward;
        progress = forward ? 1 : 0;
        lastTick = nanoClock.getAsLong();
    }

    public double progress() { update(); return progress; }
    public double value() { return easing.apply(progress()); }
    public boolean finished() { return progress() == (forward ? 1 : 0); }

    private void update() {
        long now = nanoClock.getAsLong();
        long elapsed = now - lastTick;
        // nanoTime is monotonic; ignoring a reversed injected clock keeps tests and callers safe.
        if (elapsed < 0) return;
        lastTick = now;
        if (durationNanos == 0) {
            progress = forward ? 1 : 0;
        } else {
            double step = (double) elapsed / durationNanos;
            progress = Math.clamp(progress + (forward ? step : -step), 0, 1);
        }
    }

    private static long durationNanos(long milliseconds) {
        if (milliseconds < 0) throw new IllegalArgumentException("Negative animation duration");
        return Math.multiplyExact(milliseconds, 1_000_000L);
    }
}
