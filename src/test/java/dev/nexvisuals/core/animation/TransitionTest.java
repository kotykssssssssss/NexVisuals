package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class TransitionTest {
    @Test void progressDependsOnElapsedTimeNotNumberOfFrames() {
        AtomicLong clock = new AtomicLong();
        Transition coarse = new Transition(1000, Easing.LINEAR, clock::get);
        Transition fine = new Transition(1000, Easing.LINEAR, clock::get);
        coarse.target(true);
        fine.target(true);
        for (int frame = 1; frame <= 50; frame++) {
            clock.set(frame * 10_000_000L);
            fine.target(true);
            fine.value();
        }
        assertEquals(0.5, coarse.progress(), 1e-9);
        assertEquals(coarse.progress(), fine.progress(), 1e-9);
        clock.set(2_000_000_000L);
        assertEquals(1, coarse.value());
        assertTrue(coarse.finished());
    }

    @Test void reverseMaintainsContinuityAndRunsBackToZero() {
        AtomicLong clock = new AtomicLong();
        Transition transition = new Transition(1000, Easing.OUT_CUBIC, clock::get);
        transition.target(true);
        clock.set(400_000_000L);
        double before = transition.value();
        transition.target(false);
        assertEquals(before, transition.value());
        clock.set(600_000_000L);
        assertEquals(0.2, transition.progress(), 1e-9);
        clock.set(800_000_000L);
        assertEquals(0, transition.progress(), 1e-9);
        assertTrue(transition.finished());
    }

    @Test void zeroDurationAndSnapAreImmediate() {
        AtomicLong clock = new AtomicLong();
        Transition transition = new Transition(0, Easing.IN_OUT_CUBIC, clock::get);
        transition.target(true);
        assertEquals(1, transition.value());
        transition.target(false);
        assertEquals(0, transition.value());
        transition.setDurationMillis(1000);
        transition.snap(true);
        assertEquals(1, transition.progress());
        assertThrows(IllegalArgumentException.class, () -> transition.setDurationMillis(-1));
    }

    @Test void durationChangesKeepCurrentProgressAndReversedClockIsIgnored() {
        AtomicLong clock = new AtomicLong();
        Transition transition = new Transition(1000, Easing.LINEAR, clock::get);
        transition.target(true);
        clock.set(250_000_000L);
        transition.setDurationMillis(500);
        assertEquals(0.25, transition.progress());
        clock.set(500_000_000L);
        assertEquals(0.75, transition.progress());
        clock.set(400_000_000L);
        assertEquals(0.75, transition.progress());
        transition.setDurationMillis(0);
        assertEquals(1, transition.progress());
    }

    @Test void easingHasClampedEndpointsAndMonotonicProgress() {
        for (Easing easing : Easing.values()) {
            assertEquals(0, easing.apply(-1));
            assertEquals(1, easing.apply(2));
            double previous = 0;
            for (int step = 0; step <= 100; step++) {
                double current = easing.apply(step / 100.0);
                assertTrue(current >= previous);
                previous = current;
            }
            assertThrows(IllegalArgumentException.class, () -> easing.apply(Double.NaN));
        }
    }
}
