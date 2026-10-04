package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MotionTrailTest {
    @Test void samplesOnlyObservedPositionsAndInterpolatesTheirSegment() {
        var path = new MotionTrail();
        assertEquals(0, path.observe(2, 3, 4, 4, 48));
        assertEquals(4, path.observe(3, 3, 4, 4, 48));
        assertEquals(2, path.x(0)); assertEquals(2.5, path.x(.5)); assertEquals(3, path.x(1));
        assertEquals(3, path.y(.5)); assertEquals(4, path.z(.5));
        assertEquals(2, path.x(-1)); assertEquals(3, path.x(2));
    }
    @Test void hiddenIntervalsAndTeleportsBreakHistory() {
        var path = new MotionTrail();
        path.observe(0, 0, 0, 4, 48);
        assertEquals(0, path.observe(20, 0, 0, 4, 48));
        assertEquals(2, path.observe(20.5, 0, 0, 4, 48));
        path.clear();
        assertEquals(0, path.observe(200, 0, 0, 4, 48));
        assertEquals(4, path.observe(201, 0, 0, 4, 48));
    }
    @Test void aCrowdedSceneCannotExceedTheSharedPerTickBudget() {
        int budget = 48, emitted = 0;
        for (int i = 0; i < 32; i++) {
            var path = new MotionTrail();
            path.observe(0, 0, 0, 12, budget);
            int count = path.observe(2, 0, 0, 12, budget);
            assertTrue(count >= 0 && count <= 8 && count <= budget);
            emitted += count; budget -= count;
        }
        assertEquals(48, emitted); assertEquals(0, budget);
    }
    @Test void exhaustedBudgetStillAdvancesTheObservedPositionWithoutCatchUpBursts() {
        var path = new MotionTrail();
        path.observe(0, 0, 0, 4, 48);
        assertEquals(0, path.observe(2, 0, 0, 4, 0));
        assertEquals(2, path.observe(2.5, 0, 0, 4, 48));
        assertEquals(2, path.x(0));
        assertEquals(0, path.observe(2.5, 0, 0, 4, 48));
        assertEquals(0, path.observe(3, 0, 0, 4, -10));
    }
    @Test void invalidCoordinatesAreDiscardedAndDoNotPoisonTheNextTrail() {
        var path = new MotionTrail();
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            path.observe(0, 0, 0, 4, 48);
            assertEquals(0, path.observe(value, 0, 0, 4, 48));
            assertEquals(0, path.observe(1, 0, 0, 4, 48));
            assertEquals(4, path.observe(2, 0, 0, 4, 48));
        }
    }
}
