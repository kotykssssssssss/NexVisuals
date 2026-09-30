package dev.nexvisuals.core.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleLayoutTest {
    @Test void demoNormalAndDevelopmentMenusFitMinecraftGuiSizes() {
        for (int[] size : new int[][]{{320,240},{321,241},{427,240},{640,360},{854,480},{960,540},{1920,1080}}) {
            for (var alignment : ConsoleLayout.Alignment.values()) for (int rows=2; rows<=4; rows++) {
                var layout = ConsoleLayout.of(size[0],size[1],alignment,rows);
                var panel = layout.panel();
                assertTrue(panel.x() >= 0 && panel.x()+panel.width() <= size[0]);
                assertTrue(layout.logoY() >= 4);
                assertTrue(layout.logoY()+51+4 <= panel.y(), "Logo must not cover panel: "+layout);
                assertTrue(layout.centerX()-128 >= 0 && layout.centerX()+128 <= size[0]);
                assertTrue(panel.y()+panel.height() <= size[1]-18, "Keep version/credits accessible");
                int previousBottom = panel.y()+20;
                for (int row=0; row<rows; row++) {
                    var button = layout.primary(row);
                    assertTrue(button.y() >= previousBottom+(row==0 ? 0 : layout.gap()));
                    assertTrue(button.x() > panel.x() && button.x()+button.width() < panel.x()+panel.width());
                    previousBottom = button.y()+button.height();
                }
                assertTrue(layout.utilityY() >= previousBottom+layout.gap());
                assertTrue(layout.customY() >= layout.utilityY()+20+layout.gap());
                assertTrue(layout.customY()+20 < panel.y()+panel.height());
            }
        }
    }

    @Test void leftCenterRightAreDistinctAndSymmetricOnAWideWindow() {
        var left = ConsoleLayout.of(960,540,ConsoleLayout.Alignment.LEFT,3);
        var center = ConsoleLayout.of(960,540,ConsoleLayout.Alignment.CENTER,3);
        var right = ConsoleLayout.of(960,540,ConsoleLayout.Alignment.RIGHT,3);
        assertTrue(left.centerX()<center.centerX() && center.centerX()<right.centerX());
        assertEquals(480, center.centerX());
        assertEquals(960, left.centerX()+right.centerX());
        assertEquals(left.panel().y(), right.panel().y());
    }
}
