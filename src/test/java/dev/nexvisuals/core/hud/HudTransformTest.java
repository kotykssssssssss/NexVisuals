package dev.nexvisuals.core.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudTransformTest {
    @Test void compactBarsRetainTheirSharedCenterAndRelativeSpacing() {
        for (int width : new int[] {320, 853, 960, 1280}) for (double scale : new double[] {.5, .8, 1, 1.75}) {
            int height=540, center=width/2;
            HudTransform t=HudTransform.bottomCenter(width,height,scale,0,0);
            assertEquals(center,(t.x(center-91)+t.x(center+91))/2,1e-9);
            assertEquals(182*scale,t.x(center+91)-t.x(center-91),1e-9);
            assertEquals(10*scale,t.y(height-39)-t.y(height-49),1e-9);
            assertEquals(7*scale,t.y(height-22)-t.y(height-29),1e-9);
            assertEquals(height,t.y(height),1e-9);
        }
    }
    @Test void identityPreservesVanillaAndOffsetsMoveEveryPointEqually() {
        HudTransform t=HudTransform.bottomCenter(960,540,1,17,-8);
        assertEquals(406,t.x(389)); assertEquals(493,t.y(501));
        var identity=HudTransform.bottomCenter(960,540,1,0,0);
        assertEquals(389,identity.x(389)); assertEquals(501,identity.y(501));
    }
    @Test void editorPlacementAndClampingTranslateTheWholeGroup() {
        var t=HudTransform.placed(.8,359,460,12,18);
        assertEquals(12,t.x(359)); assertEquals(18,t.y(460),1e-9);
        var clamped=HudTransform.bottomCenter(320,180,1,1000,-1000).clamped(320,180,39,100,242,80);
        assertEquals(78,clamped.x(39)); assertEquals(0,clamped.y(100));
        assertEquals(10,clamped.y(110)-clamped.y(100));
    }
    @Test void invalidScaleOrCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new HudTransform(0,0,0));
        assertThrows(IllegalArgumentException.class,()->new HudTransform(1,Double.NaN,0));
    }
}
