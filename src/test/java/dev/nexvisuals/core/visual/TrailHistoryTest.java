package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrailHistoryTest {
    @Test void samplingIsBoundedAndPreservesOrderAfterWrapping() {
        TrailHistory history=new TrailHistory(4);
        for(int i=0;i<20;i++) assertTrue(history.point(i*50,i,1,2,.01,3));
        assertEquals(4,history.size());assertEquals(16,history.coordinate(0,0));assertEquals(19,history.coordinate(3,0));
        assertEquals(800,history.time(0));assertEquals(3,history.distance(3)-history.distance(0));
    }
    @Test void stationarySamplesDoNotRefreshTailAndExpiryClearsIt() {
        TrailHistory history=new TrailHistory(8);
        history.point(0,0,0,0,.01,3);history.point(50,1,0,0,.01,3);
        assertFalse(history.point(500,1,0,0,.01,3));assertEquals(2,history.size());
        history.trim(600,200,8);assertEquals(0,history.size());
    }
    @Test void teleportBackwardsTimeAndNonfiniteCoordinatesBreakThePath() {
        TrailHistory history=new TrailHistory(8);
        history.point(100,0,0,0,.01,3);history.point(150,1,0,0,.01,3);
        history.point(200,100,0,0,.01,3);assertEquals(1,history.size());assertEquals(0,history.distance(0));
        history.point(50,101,0,0,.01,3);assertEquals(1,history.size());assertEquals(50,history.time(0));
        assertFalse(history.point(100,Double.NaN,0,0,.01,3));assertEquals(0,history.size());
        history.point(500,0,0,0,.01,3);history.trim(499,300,8);assertEquals(0,history.size());
    }
    @Test void travelledLengthTrimsCurvedPathsAndEitherBladeEdgeCanTriggerSampling() {
        TrailHistory history=new TrailHistory(8);
        history.edges(0,0,0,0,0,1,0,.1,3);
        assertTrue(history.edges(50,0,0,0,1,1,0,.1,3));
        history.edges(100,0,0,0,1,2,0,.1,3);history.trim(100,1000,1.1);
        assertEquals(2,history.size());assertEquals(50,history.time(0));
    }
    @Test void invalidLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new TrailHistory(1));
        TrailHistory history=new TrailHistory(8);
        assertThrows(IllegalArgumentException.class,()->history.point(0,0,0,0,Double.NaN,3));
        assertThrows(IllegalArgumentException.class,()->history.trim(0,0,1));
    }
}
