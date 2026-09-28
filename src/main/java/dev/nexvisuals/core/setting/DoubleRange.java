package dev.nexvisuals.core.setting;

public record DoubleRange(double lower, double upper) {
    public DoubleRange {
        if (!Double.isFinite(lower) || !Double.isFinite(upper)) {
            throw new IllegalArgumentException("Range values must be finite");
        }
    }
}
