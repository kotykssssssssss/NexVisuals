package dev.nexvisuals.core.ui;

/** Normalized top-left position within the available workspace, independent of GUI scale. */
public record PanelPosition(double x, double y, boolean collapsed) {
    public PanelPosition {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Panel position must be finite");
        x = Math.clamp(x, 0, 1); y = Math.clamp(y, 0, 1);
    }
}
