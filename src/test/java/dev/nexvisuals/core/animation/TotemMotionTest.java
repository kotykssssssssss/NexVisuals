package dev.nexvisuals.core.animation;

import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TotemMotionTest {
    @Test void customStylesHaveDistinctPosesAndSmoothOpeningClosingEnvelopes() {
        var frames=new HashSet<TotemMotion.Pose>();
        for(var style:TotemMotion.Style.values()) {
            assertTrue(frames.add(TotemMotion.sample(style,.37,Easing.OUT_CUBIC,1,1)));
            if(style==TotemMotion.Style.VANILLA) continue;
            assertEquals(0,TotemMotion.sample(style,0,Easing.OUT_CUBIC,1,1).scale());
            assertEquals(0,TotemMotion.sample(style,1,Easing.OUT_CUBIC,1,1).scale(),1e-10);
            for(int i=0;i<=100;i++) {
                var pose=TotemMotion.sample(style,i/100.0,Easing.IN_OUT_CUBIC,2,3);
                assertTrue(Double.isFinite(pose.yaw()));assertTrue(pose.scale()>=0 && pose.scale()<=1.4);
            }
        }
    }
    @Test void progressIsClampedAndSpinReachesConfiguredTurns() {
        assertEquals(TotemMotion.sample(TotemMotion.Style.SPIN,1,Easing.LINEAR,1,2),
                TotemMotion.sample(TotemMotion.Style.SPIN,20,Easing.LINEAR,1,2));
        assertEquals(720,TotemMotion.sample(TotemMotion.Style.SPIN,1,Easing.LINEAR,1,2).yaw());
        assertEquals(0,TotemMotion.sample(TotemMotion.Style.SPIN,Double.NaN,Easing.LINEAR,1,2).scale());
    }
    @Test void queryingSwingActivityDoesNotRetriggerOrExtendTimeline() {
        SwingTimeline timeline=new SwingTimeline();
        assertFalse(timeline.active(0,300));timeline.start(1,10_000_000);
        for(long now=10_000_000;now<310_000_000;now+=1_000_000) assertTrue(timeline.active(now,300));
        assertFalse(timeline.active(310_000_000,300));assertFalse(timeline.active(9_000_000,300));
        timeline.reset();assertFalse(timeline.active(100_000_000,300));
    }
}
