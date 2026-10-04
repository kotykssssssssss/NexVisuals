package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static dev.nexvisuals.core.visual.UseVisualTracker.Event.*;
import static org.junit.jupiter.api.Assertions.*;

class UseVisualTrackerTest {
    @Test void completionRequiresObservedCountdownAndCannotRepeat() {
        var tracker=new UseVisualTracker();Object world=new Object(),item=new Object();
        assertEquals(ACTIVE,tracker.sample(world,item,3,true));
        assertEquals(ACTIVE,tracker.sample(world,item,2,true));
        assertEquals(ACTIVE,tracker.sample(world,item,1,true));
        assertEquals(FINISHED,tracker.sample(world,null,0,false));
        assertEquals(NONE,tracker.sample(world,null,0,false));
    }
    @Test void cancellationsWorldChangesAndRestartedUseDoNotProduceAFlourish() {
        var tracker=new UseVisualTracker();Object world=new Object(),item=new Object();
        tracker.sample(world,item,20,true);tracker.sample(world,item,19,true);
        assertEquals(NONE,tracker.sample(world,null,0,false));
        tracker.sample(world,item,2,true);tracker.sample(world,item,1,true);
        assertEquals(NONE,tracker.sample(new Object(),null,0,false));
        tracker.sample(world,item,1,true);assertEquals(NONE,tracker.sample(world,null,0,false));
        tracker.reset();assertEquals(NONE,tracker.sample(world,null,0,false));
    }
}
