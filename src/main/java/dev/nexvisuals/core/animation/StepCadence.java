package dev.nexvisuals.core.animation;

/** Distance-based stepping, at most two visual footsteps per tick; no retained position history. */
public final class StepCadence {
    private double remainder;
    public int advance(double distance, double stride) {
        if (!Double.isFinite(distance) || distance<0 || distance>3 || !Double.isFinite(stride) || stride<=0) {
            reset(); return 0;
        }
        remainder+=distance;
        int steps=Math.min(2,(int)(remainder/stride));
        remainder%=stride;
        return steps;
    }
    public void reset() { remainder=0; }
}
