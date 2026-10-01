package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponSweepTest {
    private boolean sample(WeaponSweep sweep,long now,double x) { return sweep.sample(now,x,0,-1,x,.4,-1); }
    @Test void separateSwingsNeverGetConnectedAndSameTickDoesNotEraseTheSweep() {
        var sweep=new WeaponSweep();sweep.start(1);sample(sweep,0,0);sample(sweep,10,.1);
        sweep.start(1);assertEquals(2,sweep.history().size());
        sweep.start(2);assertEquals(0,sweep.history().size());sample(sweep,20,.5);assertEquals(1,sweep.history().size());
    }
    @Test void samplingHasAFixedCapacityRateAndLifetime() {
        var sweep=new WeaponSweep();
        for(int i=0;i<1000;i++) sample(sweep,i,i*.001);
        assertEquals(48,sweep.history().size());assertEquals(990,sweep.history().time(47));
        assertFalse(sample(sweep,995,1));sweep.trim(1400,350);assertEquals(0,sweep.history().size());
    }
    @Test void poseJumpsOrMissingFramesBreakTheRibbon() {
        var sweep=new WeaponSweep();sample(sweep,0,0);sample(sweep,10,.1);sample(sweep,20,2);
        assertEquals(1,sweep.history().size());sample(sweep,200,2.1);assertEquals(1,sweep.history().size());
        sample(sweep,190,2.2);assertEquals(1,sweep.history().size());
    }
    @Test void probesCannotCreateSheetsBehindTheCameraAndStationaryPosesExpire() {
        var sweep=new WeaponSweep();sample(sweep,0,0);sample(sweep,10,.1);
        assertFalse(sweep.sample(20,0,0,.2,0,1,.2));assertEquals(0,sweep.history().size());
        sample(sweep,40,0);for(int i=50;i<=300;i+=10) sample(sweep,i,0);
        assertEquals(1,sweep.history().size());assertEquals(40,sweep.history().time(0));
        sweep.trim(301,150);assertEquals(0,sweep.history().size());
    }
}
