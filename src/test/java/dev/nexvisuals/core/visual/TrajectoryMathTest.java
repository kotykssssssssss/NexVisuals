package dev.nexvisuals.core.visual;

import dev.nexvisuals.core.visual.TrajectoryMath.*;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TrajectoryMathTest {
    private final TrajectoryMath math = new TrajectoryMath();
    private final Collision air = (x,y,z,ex,ey,ez,result) -> {};

    @Test void bowChargeAndNormalizedLaunchIncludeOnlyExplicitInheritedMovement() {
        assertEquals(0, TrajectoryMath.bowPower(-1));
        assertEquals(5f/12, TrajectoryMath.bowPower(10));
        assertEquals(1, TrajectoryMath.bowPower(20));
        assertEquals(1, TrajectoryMath.bowPower(200));
        assertEquals(new Launch(3,64,5,.2,.1,3.3),
                TrajectoryMath.launch(3,64,5,0,0,2,3,.2,.1,.3));
        assertEquals(new Launch(3,64,5,0,0,3), TrajectoryMath.launch(3,64,5,0,0,2,3,0,0,0));
        assertNull(TrajectoryMath.launch(0,0,0,0,0,0,3,0,0,0));
        assertNull(TrajectoryMath.launch(0,0,0,0,0,1,Double.NaN,0,0,0));
    }
    @Test void arrowsMoveBeforeDragAndGravityWhileThrowablesApplyBothBeforeMoving() {
        var launch = new Launch(0,64,0,1,0,0);
        var arrow = math.predict(launch, Physics.ARROW, 2, 100, air);
        assertEquals(1, arrow.coordinate(1,0));
        assertEquals(64, arrow.coordinate(1,1));
        assertEquals(1+(double).99F, arrow.coordinate(2,0), 1e-12);
        assertEquals(63.95, arrow.coordinate(2,1), 1e-12);
        var ball = math.predict(launch, Physics.THROWABLE, 2, 100, air);
        assertEquals((double).99F, ball.coordinate(1,0), 1e-12);
        assertEquals(64-.03*.99F, ball.coordinate(1,1), 1e-12);
        var potion = math.predict(launch, Physics.POTION, 1, 100, air);
        assertEquals(64-.05*.99F, potion.coordinate(1,1), 1e-12);
    }
    @Test void firstBlockHitStopsAtExactCollisionAndKeepsItsFaceNormal() {
        var calls = new AtomicInteger();
        var path = math.predict(new Launch(0,64,0,3,0,0),Physics.ARROW,120,160,(x,y,z,ex,ey,ez,result)-> {
            calls.incrementAndGet();
            if (ex >= 4) result.block(4, y+(ey-y)*(4-x)/(ex-x),0,-1,0,0);
        });
        assertEquals(End.BLOCK,path.end()); assertEquals(3,path.points()); assertEquals(2,calls.get());
        assertEquals(4,path.coordinate(2,0)); assertEquals(-1,path.normal(0)); assertEquals(0,path.normal(1));
    }
    @Test void uncertaintyStopsBeforeTheUnknownSegmentAndCannotReuseAnOldImpact() {
        math.predict(new Launch(0,64,0,1,0,0),Physics.ARROW,2,20,(x,y,z,ex,ey,ez,r)->r.block(.5,64,0,1,0,0));
        var uncertain = math.predict(new Launch(0,64,0,1,0,0),Physics.ARROW,120,160,(x,y,z,ex,ey,ez,r)-> {
            if (ex > 1) r.contact = Contact.UNCERTAIN;
        });
        assertEquals(End.UNCERTAIN,uncertain.end()); assertEquals(2,uncertain.points());
        assertEquals(1,uncertain.coordinate(1,0)); assertEquals(0,uncertain.normal(0));
        var clear = math.predict(new Launch(0,64,0,1,0,0),Physics.ARROW,1,20,air);
        assertEquals(End.TIME_LIMIT,clear.end()); assertEquals(0,clear.normal(0));
    }
    @Test void travelDistanceClipsTheLastSegmentAndTickBudgetHasAHardCap() {
        var shortPath = math.predict(new Launch(0,64,0,3,0,0),Physics.ARROW,120,1.5,air);
        assertEquals(End.DISTANCE_LIMIT,shortPath.end()); assertEquals(2,shortPath.points());
        assertEquals(1.5,shortPath.coordinate(1,0));
        var bounded = math.predict(new Launch(0,100,0,.01,0,0),Physics.THROWABLE,Integer.MAX_VALUE,Double.MAX_VALUE,air);
        assertEquals(TrajectoryMath.MAX_STEPS+1,bounded.points()); assertEquals(End.TIME_LIMIT,bounded.end());
        var distanceCapped = math.predict(new Launch(0,100,0,7,0,0),Physics.ARROW,120,9999,air);
        double travelled = 0;
        for (int i=1; i<distanceCapped.points(); i++) {
            double dx=distanceCapped.coordinate(i,0)-distanceCapped.coordinate(i-1,0);
            double dy=distanceCapped.coordinate(i,1)-distanceCapped.coordinate(i-1,1);
            travelled+=Math.hypot(dx,dy);
        }
        assertEquals(160,travelled,1e-9); assertEquals(End.DISTANCE_LIMIT,distanceCapped.end());
    }
    @Test void invalidAndUnreasonableLaunchesNeverQueryTheWorld() {
        Collision forbidden = (x,y,z,ex,ey,ez,result) -> fail("Unexpected world query");
        assertSame(Path.EMPTY,math.predict(null,Physics.ARROW,80,100,forbidden));
        assertSame(Path.EMPTY,math.predict(new Launch(Double.NaN,0,0,1,0,0),Physics.ARROW,80,100,forbidden));
        assertSame(Path.EMPTY,math.predict(new Launch(0,0,0,1,0,0),Physics.ARROW,0,100,forbidden));
        assertSame(Path.EMPTY,math.predict(new Launch(0,0,0,1,0,0),Physics.ARROW,80,Double.POSITIVE_INFINITY,forbidden));
        var excessive = math.predict(new Launch(0,0,0,9,0,0),Physics.ARROW,80,100,forbidden);
        assertEquals(End.UNCERTAIN,excessive.end()); assertEquals(1,excessive.points());
    }
    @Test void publishedSnapshotsRemainImmutableAcrossTheNextPrediction() {
        var first=math.predict(new Launch(0,64,0,1,0,0),Physics.ARROW,2,100,air);
        math.predict(new Launch(100,90,30,0,1,0),Physics.POTION,1,100,air);
        assertEquals(0,first.coordinate(0,0)); assertEquals(64,first.coordinate(0,1)); assertEquals(3,first.points());
    }
}
