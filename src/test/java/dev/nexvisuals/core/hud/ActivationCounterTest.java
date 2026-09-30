package dev.nexvisuals.core.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActivationCounterTest {
    @Test void onlyExplicitRecordsIncreaseTheCounterAndElapsedTimeIsIndependentOfFrames() {
        var counter=new ActivationCounter();Object world=new Object();
        counter.observeLevel(world,true);assertEquals(0,counter.count());
        counter.record(world,1_000_000_000L,true);counter.record(world,2_000_000_000L,true);
        for(int i=0;i<240;i++) counter.observeLevel(world,true);
        assertEquals(2,counter.count());assertEquals(3,counter.elapsedSeconds(5_900_000_000L));
        assertEquals(0,counter.elapsedSeconds(1_000_000_000L));
        counter.reset();assertEquals(0,counter.count());assertEquals(0,counter.elapsedSeconds(9_000_000_000L));
    }
    @Test void worldResetAndExplicitCarryModeDoNotConfuseInventoryCountsWithActivations() {
        var counter=new ActivationCounter();Object world=new Object(),next=new Object();
        counter.record(world,1,true);counter.observeLevel(next,true);assertEquals(0,counter.count());
        counter.record(next,2,true);counter.observeLevel(world,false);assertEquals(1,counter.count());
        counter.record(world,3,false);assertEquals(2,counter.count());
        counter.record(null,4,false);assertEquals(2,counter.count());
        counter.observeLevel(null,true);assertEquals(0,counter.count());
    }
}
