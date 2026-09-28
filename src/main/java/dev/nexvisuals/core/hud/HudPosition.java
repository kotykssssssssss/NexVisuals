package dev.nexvisuals.core.hud;

import java.util.Objects;

/** The normalized screen position of the chosen element anchor, independent of GUI scale. */
public record HudPosition(double x, double y, Anchor anchor) {
    public HudPosition {
        if (!Double.isFinite(x) || !Double.isFinite(y)) throw new IllegalArgumentException("Position must be finite");
        x = Math.clamp(x, 0, 1);
        y = Math.clamp(y, 0, 1);
        Objects.requireNonNull(anchor);
    }

    public record PixelPosition(int x, int y) { }

    public PixelPosition resolve(int viewportWidth, int viewportHeight, int elementWidth, int elementHeight) {
        validateDimensions(viewportWidth, viewportHeight, elementWidth, elementHeight);
        int left = (int) Math.round(x * viewportWidth - anchor.x() * elementWidth);
        int top = (int) Math.round(y * viewportHeight - anchor.y() * elementHeight);
        return new PixelPosition(Math.clamp(left, 0, Math.max(0, viewportWidth - elementWidth)),
                Math.clamp(top, 0, Math.max(0, viewportHeight - elementHeight)));
    }

    public static HudPosition fromTopLeft(int left, int top, int viewportWidth, int viewportHeight,
                                         int elementWidth, int elementHeight, Anchor anchor) {
        validateDimensions(viewportWidth, viewportHeight, elementWidth, elementHeight);
        Objects.requireNonNull(anchor);
        int clampedLeft = Math.clamp(left, 0, Math.max(0, viewportWidth - elementWidth));
        int clampedTop = Math.clamp(top, 0, Math.max(0, viewportHeight - elementHeight));
        return new HudPosition((clampedLeft + anchor.x() * elementWidth) / viewportWidth,
                (clampedTop + anchor.y() * elementHeight) / viewportHeight, anchor);
    }

    private static void validateDimensions(int width, int height, int elementWidth, int elementHeight) {
        if (width <= 0 || height <= 0 || elementWidth < 0 || elementHeight < 0) {
            throw new IllegalArgumentException("Viewport must be positive and element dimensions nonnegative");
        }
    }
}
