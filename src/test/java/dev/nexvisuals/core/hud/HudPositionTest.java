package dev.nexvisuals.core.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudPositionTest {
    @Test void eachAnchorRoundTripsDraggingCoordinates() {
        for (Anchor anchor : Anchor.values()) {
            HudPosition position = HudPosition.fromTopLeft(160, 70, 640, 360, 100, 20, anchor);
            assertEquals(new HudPosition.PixelPosition(160, 70), position.resolve(640, 360, 100, 20));
        }
    }

    @Test void cornerAndCenterAnchorsSurviveViewportResize() {
        HudPosition bottomRight = new HudPosition(1, 1, Anchor.BOTTOM_RIGHT);
        assertEquals(new HudPosition.PixelPosition(540, 340), bottomRight.resolve(640, 360, 100, 20));
        assertEquals(new HudPosition.PixelPosition(1180, 700), bottomRight.resolve(1280, 720, 100, 20));
        HudPosition center = new HudPosition(0.5, 0.5, Anchor.CENTER);
        assertEquals(new HudPosition.PixelPosition(270, 170), center.resolve(640, 360, 100, 20));
        assertEquals(new HudPosition.PixelPosition(590, 350), center.resolve(1280, 720, 100, 20));
    }

    @Test void oversizedAndOffscreenElementsStayReachable() {
        assertEquals(new HudPosition.PixelPosition(0, 0), new HudPosition(-5, -3, Anchor.CENTER).resolve(640, 360, 100, 20));
        assertEquals(new HudPosition.PixelPosition(0, 0), new HudPosition(1, 1, Anchor.BOTTOM_RIGHT).resolve(50, 20, 100, 40));
        HudPosition clamped = HudPosition.fromTopLeft(900, -50, 640, 360, 100, 20, Anchor.TOP_LEFT);
        assertEquals(new HudPosition.PixelPosition(540, 0), clamped.resolve(640, 360, 100, 20));
    }

    @Test void invalidViewportAndNonFiniteCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new HudPosition(Double.NaN, 0, Anchor.TOP_LEFT));
        HudPosition position = new HudPosition(0, 0, Anchor.TOP_LEFT);
        assertThrows(IllegalArgumentException.class, () -> position.resolve(0, 360, 100, 20));
        assertThrows(IllegalArgumentException.class, () -> position.resolve(640, 360, -1, 20));
    }
}
