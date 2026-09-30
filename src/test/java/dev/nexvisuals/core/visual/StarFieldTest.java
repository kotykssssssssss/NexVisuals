package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StarFieldTest {
    @Test void distributionIsDeterministicBoundedAndOnTheUnitSphere() {
        float[] field=StarField.generate(4000);
        assertArrayEquals(field,StarField.generate(5000));
        assertEquals(20_000,field.length);
        for(int i=0;i<field.length;i+=5) {
            assertEquals(1,field[i]*field[i]+field[i+1]*field[i+1]+field[i+2]*field[i+2],.000001);
            assertTrue(field[i+3]>=0 && field[i+3]<=1);
            assertTrue(field[i+4]>=.55 && field[i+4]<=1);
        }
        assertEquals(0,StarField.generate(-1).length);
        assertArrayEquals(java.util.Arrays.copyOf(field,500),StarField.generate(100));
    }
    @Test void independentTwinkleNeverExceedsBrightnessAndCanBeDisabled() {
        for(int i=0;i<100;i++) {
            double value=StarField.twinkle(i*.1,1.4,.23,.4);
            assertTrue(value>=.6 && value<=1);
            assertEquals(1,StarField.twinkle(i,1.4,.23,0));
        }
        assertNotEquals(StarField.twinkle(1,1.4,.1,.4),StarField.twinkle(1,1.4,.6,.4));
    }
}
