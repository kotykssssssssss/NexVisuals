package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroundMotionTest {
    @Test void actualTakeoffAndDescentProduceOneEventEach() {
        var motion=new GroundMotion();
        assertEquals(GroundMotion.Event.NONE,motion.sample(0,64,0,true));
        assertEquals(GroundMotion.Event.JUMP,motion.sample(0,64.42,0,false));
        assertEquals(GroundMotion.Event.NONE,motion.sample(0,64.7,0,false));
        assertEquals(GroundMotion.Event.NONE,motion.sample(0,64.4,0,false));
        assertEquals(GroundMotion.Event.LAND,motion.sample(0,64,0,true));
        assertTrue(motion.impact()>0 && motion.impact()<1);
        assertEquals(GroundMotion.Event.NONE,motion.sample(0,64,0,true));
    }
    @Test void WalkingOffALedgeIsNotAJumpButCanLand() {
        var motion=new GroundMotion();motion.sample(0,64,0,true);
        assertEquals(GroundMotion.Event.NONE,motion.sample(.2,63.9,0,false));
        motion.sample(.3,63.4,0,false);
        assertEquals(GroundMotion.Event.LAND,motion.sample(.4,63,0,true));
    }
    @Test void teleportsWorldResetAndSmallStepsDoNotInventEvents() {
        var motion=new GroundMotion();motion.sample(0,64,0,true);
        assertEquals(GroundMotion.Event.NONE,motion.sample(100,70,0,false));
        assertEquals(GroundMotion.Event.NONE,motion.sample(100,70,0,true));
        assertEquals(GroundMotion.Event.NONE,motion.sample(100,70.04,0,false));
        assertEquals(GroundMotion.Event.NONE,motion.sample(100,70,0,true));
        motion.reset();assertEquals(GroundMotion.Event.NONE,motion.sample(0,64,0,true));
    }
}
