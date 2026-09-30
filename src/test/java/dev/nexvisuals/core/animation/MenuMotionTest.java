package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class MenuMotionTest {
    @Test void stylesProduceDistinctPathsAndStillKeepsTheChosenHeading() {
        var paths = new HashSet<MenuMotion.Angles>();
        for (var style : MenuMotion.Style.values()) paths.add(MenuMotion.angles(style, 12, 14, 10, 20, false));
        assertEquals(4, paths.size());
        assertEquals(new MenuMotion.Angles(10, 20), MenuMotion.angles(MenuMotion.Style.STILL, 500, 35, 10, 20, true));
    }

    @Test void swayIsBoundedAndReverseMirrorsMotionAroundTheStartingAngle() {
        for (double time = 0; time < 200; time += .25) {
            var forward = MenuMotion.angles(MenuMotion.Style.SWAY, time, 14, 10, 20, false);
            var backward = MenuMotion.angles(MenuMotion.Style.SWAY, time, 14, 10, 20, true);
            assertTrue(forward.yaw() >= 6 && forward.yaw() <= 34);
            assertEquals(40, forward.yaw()+backward.yaw(), .00001);
            assertEquals(10, forward.pitch());
        }
    }

    @Test void longRunningOrbitWrapsAndTiltRemainsBounded() {
        var angle = MenuMotion.angles(MenuMotion.Style.ORBIT, 180*10_000+100, 35, 10, 0, false);
        assertEquals(-160, angle.yaw());
        assertEquals(160, MenuMotion.angles(MenuMotion.Style.ORBIT, 100, 35, 10, 0, true).yaw());
        for (int time = 0; time < 100; time++) {
            var drift = MenuMotion.angles(MenuMotion.Style.DRIFT, time, 35, 39, 170, false);
            assertTrue(drift.pitch() >= -40 && drift.pitch() <= 40);
            assertTrue(drift.yaw() >= -180 && drift.yaw() < 180);
        }
        assertThrows(IllegalArgumentException.class, () -> MenuMotion.angles(MenuMotion.Style.DRIFT, Double.NaN, 14, 10, 0, false));
    }

    @Test void entranceStaggersRowsAndAlwaysSettlesAtTheClickableRestingPosition() {
        assertEquals(0, MenuMotion.entrance(-1, 0, 350));
        assertEquals(0, MenuMotion.entrance(69, 2, 350));
        assertTrue(MenuMotion.entrance(120, 0, 350) > MenuMotion.entrance(120, 2, 350));
        for (int row=0; row<6; row++) {
            assertEquals(1, MenuMotion.entrance(1000, row, 350));
            assertEquals(1, MenuMotion.entrance(0, row, 0));
        }
    }
}
