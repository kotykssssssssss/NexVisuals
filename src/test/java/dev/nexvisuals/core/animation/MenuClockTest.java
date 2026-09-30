package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class MenuClockTest {
    @Test void accumulatedMotionDoesNotDependOnFrameRate() {
        for (int fps : new int[]{20, 30, 60, 144}) {
            var now = new AtomicLong();
            var clock = new MenuClock(now::get);
            double elapsed = 0;
            for (int frame=1; frame<=fps*10; frame++) {
                now.set(frame*1_000_000_000L/fps);
                elapsed = clock.advance(.7, true);
            }
            assertEquals(7, elapsed, .000001);
        }
    }

    @Test void pauseAndZeroSpeedResumeWithoutCatchingUp() {
        var now = new AtomicLong();
        var clock = new MenuClock(now::get);
        now.set(100_000_000);
        assertEquals(.1, clock.advance(1, true), .00001);
        now.set(30_000_000_000L);
        assertEquals(.1, clock.advance(1, false), .00001);
        now.addAndGet(100_000_000);
        assertEquals(.2, clock.advance(1, true), .00001);
        now.addAndGet(10_000_000_000L);
        assertEquals(.2, clock.advance(0, true), .00001);
        now.addAndGet(100_000_000);
        assertEquals(.4, clock.advance(2, true), .00001);
    }

    @Test void stalledOrReversedClocksCannotCauseHugeJumps() {
        var now = new AtomicLong();
        var clock = new MenuClock(now::get);
        now.set(120_000_000_000L);
        assertEquals(.25, clock.advance(1, true));
        now.set(100);
        assertEquals(.25, clock.advance(1, true));
        now.set(120_100_000_000L);
        assertEquals(.35, clock.advance(1, true), .00001);
        assertThrows(IllegalArgumentException.class, () -> clock.advance(-1, true));
        assertThrows(IllegalArgumentException.class, () -> clock.advance(Double.NaN, true));
    }
}
