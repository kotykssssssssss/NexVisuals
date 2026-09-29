package dev.nexvisuals.core.hud;

/** One affine transform shared by related vanilla layers, so scaling preserves their spacing. */
public record HudTransform(double scale, double translateX, double translateY) {
    public HudTransform {
        if (!Double.isFinite(scale) || scale <= 0 || !Double.isFinite(translateX) || !Double.isFinite(translateY))
            throw new IllegalArgumentException("HUD transform must be finite with a positive scale");
    }

    public static HudTransform bottomCenter(int width, int height, double scale, double x, double y) {
        // Vanilla uses integer division for the center, including on odd-width windows.
        return new HudTransform(scale, (width / 2) * (1 - scale) + x, height * (1 - scale) + y);
    }

    public static HudTransform placed(double scale, double baseX, double baseY, double targetX, double targetY) {
        return new HudTransform(scale, targetX - baseX * scale, targetY - baseY * scale);
    }

    public double x(double original) { return original * scale + translateX; }
    public double y(double original) { return original * scale + translateY; }

    public HudTransform clamped(int viewportWidth, int viewportHeight, int left, int top, int width, int height) {
        double targetX = x(left), targetY = y(top);
        double clampedX = Math.clamp(targetX, 0, Math.max(0, viewportWidth - width*scale));
        double clampedY = Math.clamp(targetY, 0, Math.max(0, viewportHeight - height*scale));
        return new HudTransform(scale, translateX + clampedX - targetX, translateY + clampedY - targetY);
    }
}
