package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EffectMathTest {
    @Test void envelopesStartAndEndAtNeutralWithExactPeak() {
        for (Easing easing : Easing.values()) for (double peak : new double[]{.1, .35, .9}) {
            assertEquals(0, EffectMath.envelope(0, peak, easing));
            assertEquals(1, EffectMath.envelope(peak, peak, easing));
            assertEquals(0, EffectMath.envelope(1, peak, easing));
            for (int i=0;i<=100;i++) { double a=EffectMath.envelope(i/100.,peak,easing); assertTrue(a>=0 && a<=1); }
        }
    }
    @Test void colorInterpolationRetainsAlphaAndDoesNotOverflowSignedArgb() {
        assertEquals(0x80808080, EffectMath.color(0x00000000, 0xFFFFFFFF, .5));
        assertEquals(0xFF778899, EffectMath.color(0xFF778899, 0, -3));
        assertEquals(0xEE112233, EffectMath.color(0, 0xEE112233, 4));
        assertEquals(0, EffectMath.arc(0)); assertEquals(1, EffectMath.arc(.5)); assertEquals(0, EffectMath.arc(1));
    }
    @Test void nonfiniteProgressIsBounded() { assertEquals(0, EffectMath.unit(Double.NaN)); assertEquals(0, EffectMath.unit(Double.POSITIVE_INFINITY)); }
}
