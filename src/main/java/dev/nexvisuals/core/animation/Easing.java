package dev.nexvisuals.core.animation;

public enum Easing {
    LINEAR { @Override double curve(double t) { return t; } },
    OUT_QUAD { @Override double curve(double t) { return 1 - (1 - t) * (1 - t); } },
    OUT_CUBIC { @Override double curve(double t) { return 1 - Math.pow(1 - t, 3); } },
    IN_OUT_CUBIC {
        @Override double curve(double t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
    };

    abstract double curve(double t);

    public double apply(double progress) {
        if (!Double.isFinite(progress)) throw new IllegalArgumentException("Progress must be finite");
        return curve(Math.clamp(progress, 0, 1));
    }
}
