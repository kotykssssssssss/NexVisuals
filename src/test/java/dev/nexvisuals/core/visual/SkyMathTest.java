package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkyMathTest {
    @Test void dayNightAndTwilightWeightsAreSmoothNormalizedAndPeriodic() {
        for(int i=0;i<2000;i++) {
            double angle=i*Math.PI/1000;
            var weights=SkyMath.weights(angle,1);
            assertEquals(1,weights.day()+weights.night()+weights.sunset(),.000001);
            assertTrue(weights.day()>=0 && weights.night()>=0 && weights.sunset()>=0);
            assertEquals(weights.day(),SkyMath.weights(angle+Math.PI*2,1).day(),.000001);
            var next=SkyMath.weights(angle+.001,1);
            assertTrue(Math.abs(weights.day()-next.day())<.01);
        }
        assertEquals(1,SkyMath.weights(0,1).day());
        assertEquals(1,SkyMath.weights(Math.PI,1).night());
        assertTrue(SkyMath.weights(Math.PI/2,1).sunset()>.6);
        assertEquals(new SkyMath.Weights(1,0,0),SkyMath.weights(Math.PI,0));
    }
    @Test void palettesBlendAndSkyGradeClampsWithoutMutatingWorldLight() {
        assertEquals(0xFFFF0000,SkyMath.blend(0xFFFF0000,0xFF00FF00,0xFF0000FF,new SkyMath.Weights(1,0,0)));
        assertEquals(0xFF808000,SkyMath.blend(0xFFFF0000,0xFF00FF00,0xFF0000FF,new SkyMath.Weights(.5,.5,0)));
        assertEquals(0xFFFFFFFF,SkyMath.grade(0xFFFFFFFF,1.6,1.6,0xFFFFFFFF));
        int gray=SkyMath.grade(0xFF235ABC,1,0,0xFFFFFFFF);
        assertEquals(gray&255,(gray>>>8)&255);
        assertEquals(gray&255,(gray>>>16)&255);
    }
    @Test void extraFogNeverExtendsVanillaDistances() {
        for(float distance:new float[]{0,4,64,256,Float.MAX_VALUE}) for(double density:new double[]{-1,1,1.5,3,99}) {
            float result=SkyMath.fogDistance(distance,density);
            assertTrue(Float.isFinite(result) && result>=0 && result<=distance);
        }
        assertEquals(128,SkyMath.fogDistance(256,2));
    }
    @Test void fourPhaseCycleIsContinuousNormalizedAndPeriodicAtEveryTimeOfDay() {
        for (double influence : new double[]{0, .4, 1}) for (int i=0; i<2000; i++) {
            double angle=i*Math.PI/1000;
            var cycle=SkyMath.cycle(angle,influence);
            var next=SkyMath.cycle(angle+.001,influence);
            var wrapped=SkyMath.cycle(angle+Math.PI*2,influence);
            double[] weights={cycle.day(),cycle.morning(),cycle.sunset(),cycle.night()};
            double[] following={next.day(),next.morning(),next.sunset(),next.night()};
            double[] periodic={wrapped.day(),wrapped.morning(),wrapped.sunset(),wrapped.night()};
            assertEquals(1, java.util.Arrays.stream(weights).sum(), .000001);
            for (int phase=0; phase<weights.length; phase++) {
                assertTrue(weights[phase]>=0 && weights[phase]<=1);
                assertTrue(Math.abs(weights[phase]-following[phase])<.01);
                assertEquals(weights[phase],periodic[phase],.000001);
            }
        }
        assertEquals(new SkyMath.Cycle(1,0,0,0),SkyMath.cycle(Math.PI,0));
    }
    @Test void sunriseAndSunsetHaveDifferentColorsAndNightRetainsItsPalette() {
        var morning=SkyMath.cycle(3*Math.PI/2,1);
        var evening=SkyMath.cycle(Math.PI/2,1);
        assertTrue(morning.morning()>.6); assertEquals(0,morning.sunset());
        assertTrue(evening.sunset()>.6); assertEquals(0,evening.morning());
        int day=0xFF60A7E3, dawn=0xFFFFD4AA, dusk=0xFFFFA06F, night=0xFF171530;
        assertNotEquals(SkyMath.blendCycle(day,dawn,dusk,night,morning),SkyMath.blendCycle(day,dawn,dusk,night,evening));
        assertEquals(day,SkyMath.blendCycle(day,dawn,dusk,night,SkyMath.cycle(0,1)));
        assertEquals(night,SkyMath.blendCycle(day,dawn,dusk,night,SkyMath.cycle(Math.PI,1)));
        for (int i=0;i<2000;i++) {
            double level=SkyMath.cycleBrightness(SkyMath.cycle(i*Math.PI/1000,1),1.03,.96,1);
            assertTrue(level>=.96-.000001 && level<=1.03+.000001);
        }
    }
}
