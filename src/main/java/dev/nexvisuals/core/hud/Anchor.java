package dev.nexvisuals.core.hud;

public enum Anchor {
    TOP_LEFT(0, 0), TOP_RIGHT(1, 0), BOTTOM_LEFT(0, 1), BOTTOM_RIGHT(1, 1), CENTER(0.5, 0.5);

    private final double x;
    private final double y;
    Anchor(double x, double y) { this.x = x; this.y = y; }
    public double x() { return x; }
    public double y() { return y; }
}
