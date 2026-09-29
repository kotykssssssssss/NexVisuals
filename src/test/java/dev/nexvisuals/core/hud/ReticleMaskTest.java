package dev.nexvisuals.core.hud;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReticleMaskTest {
    private Set<String> pixels(List<ReticleMask.Span> spans, boolean outline) {
        Set<String> pixels=new HashSet<>();
        for (var s:spans) if(s.outline()==outline) for(int x=s.x();x<s.x()+s.width();x++)
            assertTrue(pixels.add(x+","+s.y()), "A pixel should be emitted once to preserve alpha");
        return pixels;
    }
    @Test void ringHasOpenCenterAndDisjointOutline() {
        var spans=ReticleMask.build(ReticleMask.Shape.CIRCLE,8,8,1,0,2);
        var fill=pixels(spans,false); var outline=pixels(spans,true);
        assertFalse(fill.contains("0,0")); assertTrue(fill.contains("8,0")); assertTrue(fill.contains("-8,0"));
        assertFalse(outline.isEmpty()); assertTrue(Collections.disjoint(fill,outline));
    }
    @Test void chevronIsDifferentAndAllGeometryIsBoundedEvenAtExtremeInputs() {
        assertNotEquals(ReticleMask.build(ReticleMask.Shape.CIRCLE,6,6,1,0,1),ReticleMask.build(ReticleMask.Shape.CHEVRON,6,6,1,0,1));
        for(var shape:ReticleMask.Shape.values()) {
            var spans=ReticleMask.build(shape,Integer.MAX_VALUE,-100,20,999,999);
            assertTrue(spans.size()<500);
            for(var s:spans) { assertTrue(Math.abs(s.x())<=54); assertTrue(Math.abs(s.y())<=54); assertTrue(s.width()>0 && s.width()<110); }
        }
    }
}
