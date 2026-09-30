package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorGradeTest {
    @Test void identityPreservesTheImageAndGrayscalePreservesLuminance() {
        var identity=new ColorGrade.Parameters(1,1,1,1,0,0);
        assertArrayEquals(new double[]{.1,.4,.8},ColorGrade.apply(.1,.4,.8,identity),.000001);
        double[] gray=ColorGrade.apply(.1,.4,.8,new ColorGrade.Parameters(1,1,0,1,0,0));
        assertEquals(gray[0],gray[1]); assertEquals(gray[0],gray[2]);
        assertEquals(.1*.2126+.4*.7152+.8*.0722,gray[0],.000001);
    }
    @Test void blackRemainsBlackAcrossAllowedExtremeCurvesAndOutputsStayBounded() {
        for(double brightness:new double[]{.65,1.4}) for(double gamma:new double[]{.8,1.2}) for(double contrast:new double[]{.65,1.5}) {
            var parameters=new ColorGrade.Parameters(brightness,contrast,1.6,gamma,1,1);
            assertArrayEquals(new double[3],ColorGrade.apply(0,0,0,parameters));
            for(double value:ColorGrade.apply(.1,.6,1,parameters)) assertTrue(Double.isFinite(value) && value>=0 && value<=1);
        }
    }
    @Test void warmAndColdProduceOppositeBalances() {
        double[] warm=ColorGrade.apply(.5,.5,.5,new ColorGrade.Parameters(1,1,1,1,1,0));
        double[] cold=ColorGrade.apply(.5,.5,.5,new ColorGrade.Parameters(1,1,1,1,-1,0));
        assertTrue(warm[0]>warm[2]); assertTrue(cold[0]<cold[2]);
        assertEquals(warm[0],cold[2],.000001);
    }
}
