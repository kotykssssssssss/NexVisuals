package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlightTrailTest {
    @Test void wingsRemainSymmetricAndRotateWithHeading() {
        for(int yaw=0;yaw<360;yaw+=45) {
            var a=FlightTrail.wing(4,2,8,yaw,.7,-1); var b=FlightTrail.wing(4,2,8,yaw,.7,1);
            assertEquals(4,(a.x()+b.x())/2,1e-9); assertEquals(8,(a.z()+b.z())/2,1e-9);
            assertEquals(1.4,Math.hypot(a.x()-b.x(),a.z()-b.z()),1e-9);
        }
    }
    @Test void idleTeleportAndInvalidMovementNeverLeaveLongStripes() {
        assertEquals(0,FlightTrail.samples(0,4)); assertEquals(0,FlightTrail.samples(9,4));
        assertEquals(0,FlightTrail.samples(Double.NaN,4));
    }
    @Test void fastMovementStillHasABoundedPairBudget() {
        assertEquals(8,FlightTrail.samples(7,12));
        assertEquals(2,FlightTrail.samples(.5,4));
    }
}
