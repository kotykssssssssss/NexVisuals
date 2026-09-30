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
}
