package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentEnvelopeTest {
    @Test void sameTimeProducesSameFadeAtSixtyAndTwoHundredFortyFps() {
        assertEquals(simulate(60),simulate(240),1e-12);
        assertEquals(1-Math.exp(-2),simulate(60),1e-12);
    }
    private double simulate(int frames) {
        var e=new EnvironmentEnvelope();double value=e.update(1,0,.5);
        for(int i=1;i<=frames;i++) value=e.update(1,(double)i/frames,.5);
        return value;
    }
    @Test void pauseRewindWorldResetAndDryingLeaveNoStaleWetness() {
        var e=new EnvironmentEnvelope();e.update(1,0,.5);
        double wet=e.update(1,.5,.5);
        assertEquals(wet,e.update(1,.5,.5));
        assertEquals(0,e.update(1,0,.5));
        e.update(1,.5,.5);double previous=1;
        for(int i=1;i<=80;i++) {double current=e.update(0,.5+i*.1,.5);assertTrue(current<=previous);previous=current;}
        assertEquals(0,previous);e.reset();assertEquals(0,e.update(1,100,.5));
    }
}
