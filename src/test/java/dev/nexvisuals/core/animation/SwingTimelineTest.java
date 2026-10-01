package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwingTimelineTest {
    @Test void durationIsCosmeticAndIndependentOfVanillaProgressAndFrameRate() {
        SwingTimeline t = new SwingTimeline();
        assertEquals(0, t.progress(0, 300));
        assertTrue(t.start(10, 1_000_000));
        assertEquals(.5, t.progress(151_000_000, 300), 1e-9);
        assertEquals(1, t.progress(301_000_000, 300));
        assertFalse(t.active(301_000_000, 300));
    }
    @Test void aNewCycleRetriggersAndDisablingClearsHistory() {
        SwingTimeline t = new SwingTimeline();
        t.start(10, 1);
        assertEquals(1.0 / 3, t.progress(100_000_001, 300), 1e-9);
        assertTrue(t.start(13, 110_000_001));
        assertEquals(0, t.progress(110_000_001, 300));
        assertEquals(.5, t.progress(260_000_001, 300), 1e-9);
        t.reset(); assertEquals(0, t.progress(300_000_001, 300));
    }
    @Test void duplicateTickAndRenderingCannotKeepRestartingASingleClick() {
        SwingTimeline t = new SwingTimeline();t.start(20,0);
        assertFalse(t.start(20,100_000_000));
        for(long now=0;now<=300_000_000;now+=4_000_000) assertEquals(now/300_000_000.0,t.progress(now,300),1e-9);
        assertEquals(1,t.progress(900_000_000,300));assertFalse(t.active(900_000_000,300));
    }
    @Test void handsAndClampedDurationsAreIndependent() {
        SwingTimeline main=new SwingTimeline(),off=new SwingTimeline();main.start(1,0);
        assertFalse(off.active(10_000_000,300));off.start(1,100_000_000);
        assertEquals(.5,main.progress(150_000_000,300));assertEquals(1.0/6,off.progress(150_000_000,300),1e-9);
        assertEquals(0,off.progress(90_000_000,300));assertEquals(1,main.progress(1_000_000,-1));
    }
}
