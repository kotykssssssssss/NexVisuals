package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StepCadenceTest {
    @Test void footstepsDependOnDistanceNotTickCount() {
        var slow=new StepCadence();var fast=new StepCadence();int a=0,b=0;
        for(int i=0;i<100;i++) a+=slow.advance(.1,.65);
        for(int i=0;i<20;i++) b+=fast.advance(.5,.65);
        assertEquals(15,a);assertEquals(a,b);
        assertEquals(0,slow.advance(0,.65));
    }
    @Test void burstsAreBoundedAndTeleportsResetTheRemainder() {
        var cadence=new StepCadence();assertEquals(2,cadence.advance(2.9,.25));
        assertEquals(0,cadence.advance(20,.25));
        assertEquals(0,cadence.advance(.1,.25));cadence.reset();
        assertEquals(0,cadence.advance(.2,.25));
        assertEquals(0,cadence.advance(Double.NaN,.25));
    }
}
