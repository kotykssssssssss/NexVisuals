package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmoothingTest {
    @Test void sameElapsedTimeHasSameResultAtDifferentFrameRates() {
        double thirty = 0, oneFortyFour = 0;
        for (int frame = 0; frame < 30; frame++) thirty = Smoothing.approach(thirty, 12, 1000.0 / 30, 180);
        for (int frame = 0; frame < 144; frame++) oneFortyFour = Smoothing.approach(oneFortyFour, 12, 1000.0 / 144, 180);
        assertEquals(Smoothing.approach(0, 12, 1000, 180), thirty, 1e-10);
        assertEquals(thirty, oneFortyFour, 1e-10);
    }
    @Test void reversingNeverOvershootsAndZeroResponseIsImmediate() {
        double value = Smoothing.approach(10, -5, 100, 200);
        assertTrue(value < 10 && value > -5);
        assertEquals(-5, Smoothing.approach(value, -5, 0, 0));
        assertEquals(value, Smoothing.approach(value, 10, 0, 200));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.approach(0, 1, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> Smoothing.approach(0, Double.NaN, 1, 20));
    }
}
