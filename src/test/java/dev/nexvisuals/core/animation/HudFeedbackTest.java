package dev.nexvisuals.core.animation;

import dev.nexvisuals.core.hud.ReticleMotion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudFeedbackTest {
    @Test void lowHealthUsesFractionOfOwnMaximumAndHasNoPulseAboveThreshold() {
        assertEquals(0,HudFeedback.lowHealth(20,20,.35));
        assertEquals(0,HudFeedback.lowHealth(7,20,.35));
        assertEquals(.5,HudFeedback.lowHealth(3.5,20,.35),1e-9);
        assertEquals(.5,HudFeedback.lowHealth(7,40,.35),1e-9);
        assertEquals(1,HudFeedback.lowHealth(-1,20,.35));
        assertEquals(0,HudFeedback.lowHealth(5,0,.35));
        assertEquals(0,HudFeedback.lowHealth(5,20,Double.NaN));
    }
    @Test void heartbeatIsTimeBasedPeriodicBoundedAndHasTwoDistinctBeats() {
        for(int frame=0;frame<=240;frame++) {
            double seconds=frame/240.0,value=HudFeedback.heartbeat(seconds,1.2);
            assertTrue(value>=0&&value<=1);assertEquals(value,HudFeedback.heartbeat(seconds+1/1.2,1.2),1e-9);
        }
        assertTrue(HudFeedback.heartbeat(.18,1)>HudFeedback.heartbeat(.3,1));
        assertTrue(HudFeedback.heartbeat(.4,1)>HudFeedback.heartbeat(.7,1));
        assertEquals(0,HudFeedback.heartbeat(Double.NaN,1));
    }
    @Test void equipmentDurabilityAndEffectTimersCoverEmptyBrokenAndInfinite() {
        assertEquals(1,HudFeedback.durability(10,0));assertEquals(0,HudFeedback.durability(500,100));
        assertEquals(.75,HudFeedback.durability(25,100));assertEquals(1,HudFeedback.durability(-10,100));
        assertEquals("0:00",HudFeedback.duration(-1,false));assertEquals("1:05",HudFeedback.duration(1300,false));
        assertEquals("∞",HudFeedback.duration(-1,true));
    }
    @Test void reticleEnvelopesReturnToRestAndAreBoundedWithoutTargetState() {
        assertEquals(0,ReticleMotion.swing(0));assertEquals(1,ReticleMotion.swing(.5),1e-9);
        assertEquals(0,ReticleMotion.swing(1),1e-9);assertEquals(0,ReticleMotion.swing(-3));
        assertEquals(1,ReticleMotion.movement(10));assertEquals(.4,ReticleMotion.movement(.1),1e-9);
        assertEquals(1,ReticleMotion.scale(0,.5));assertEquals(2,ReticleMotion.scale(3,3));
        assertEquals(ReticleMotion.breathe(.125,2),ReticleMotion.breathe(.625,2),1e-9);
    }
}
