package dev.nexvisuals.core.visual;

import dev.nexvisuals.core.animation.Easing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleMathTest {
    private ParticleTuning tuning(ParticleTuning.ColorMode mode) {
        return new ParticleTuning(mode,.2,.65,.8,.1,1,1,.94,1,0,0,1,0,0,0,false,0,.35,false,1,.15,0,1,64,false);
    }
    @Test void neutralTuningKeepsTheLegacyGradientAndZeroVariance() {
        assertEquals(0xFF808080,ParticleMath.color(ParticleTuning.LEGACY,0xFF000000,0xFFFFFFFF,.5,.3,1,0));
        for(double sample:new double[]{0,.5,1}) assertEquals(.12,ParticleMath.variance(.12,0,sample));
        assertEquals(.06,ParticleMath.variance(.12,.5,0)); assertEquals(.18,ParticleMath.variance(.12,.5,1));
    }
    @Test void colorModesHaveDistinctAndStableMeaning() {
        int a=0xCC4080FF,b=0x66703090;
        assertEquals(a,ParticleMath.color(tuning(ParticleTuning.ColorMode.STATIC),a,b,1,.9,0,0));
        assertEquals(b,ParticleMath.color(tuning(ParticleTuning.ColorMode.TWO_COLOR),a,b,0,.7,0,0));
        assertEquals(a,ParticleMath.color(tuning(ParticleTuning.ColorMode.TWO_COLOR),a,b,1,.2,0,0));
        assertEquals(b,ParticleMath.color(tuning(ParticleTuning.ColorMode.GRADIENT),a,b,1,.2,0,0));
        var random=tuning(ParticleTuning.ColorMode.RANDOM_BETWEEN);
        assertEquals(ParticleMath.color(random,a,b,0,.4,0,0),ParticleMath.color(random,a,b,1,.4,10,0));
        assertEquals(0xCCABCDEF,ParticleMath.color(tuning(ParticleTuning.ColorMode.THEME),a,b,.5,.2,0,0xFFABCDEF));
        var rainbow=tuning(ParticleTuning.ColorMode.RAINBOW);
        assertNotEquals(ParticleMath.color(rainbow,a,b,0,.4,0,0),ParticleMath.color(rainbow,a,b,0,.4,1,0));
        assertEquals(a>>>24,ParticleMath.color(rainbow,a,b,0,.4,1,0)>>>24);
    }
    @Test void fadeWindowsAndDistanceAreBoundedAndContinuous() {
        assertEquals(0,ParticleMath.envelope(0,.1,.4,Easing.LINEAR));
        assertEquals(1,ParticleMath.envelope(.2,.1,.4,Easing.LINEAR));
        assertEquals(.5,ParticleMath.envelope(.8,.1,.4,Easing.LINEAR),1e-8);
        assertEquals(0,ParticleMath.envelope(1,.1,.4,Easing.LINEAR));
        assertEquals(1,ParticleMath.distanceFade(49,10,true));
        assertEquals(.5,ParticleMath.distanceFade(8.5*8.5,10,true),1e-8);
        assertEquals(0,ParticleMath.distanceFade(100,10,false));
        assertEquals(0,ParticleMath.distanceFade(Double.NaN,10,true));
        for(int i=0;i<=100;i++) assertTrue(ParticleMath.envelope(i/100.,.4,.8,Easing.OUT_CUBIC)>=0);
    }
    @Test void lightIntensityDoesNotAlterGeometryOrExceedNativeFullbright() {
        assertEquals(0x800080,ParticleMath.light(0x800080,0));
        assertEquals(0xB800B8,ParticleMath.light(0x800080,.5));
        assertEquals(0xF000F0,ParticleMath.light(0x800080,1));
    }
}
