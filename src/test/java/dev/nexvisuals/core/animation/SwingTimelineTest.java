package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwingTimelineTest {
    @Test void durationIsCosmeticAndIndependentOfVanillaProgressAndFrameRate() {
        SwingTimeline t = new SwingTimeline();
        assertEquals(0, t.sample(0, 0, 300));
        assertEquals(0, t.sample(.1, 1_000_000, 300));
        assertEquals(.5, t.sample(.4, 151_000_000, 300), 1e-9);
        assertEquals(0, t.sample(.7, 301_000_000, 300));
    }
    @Test void aNewCycleRetriggersAndDisablingClearsHistory() {
        SwingTimeline t = new SwingTimeline();
        t.sample(.1, 1, 300); t.sample(.9, 100_000_001, 300);
        assertEquals(0, t.sample(.1, 110_000_001, 300));
        assertEquals(.5, t.sample(.3, 260_000_001, 300), 1e-9);
        t.reset(); assertEquals(0, t.sample(0, 300_000_001, 300));
    }
}
