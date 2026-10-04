package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleBudgetTest {
    @Test void allQualitiesHaveHardCapsEvenUnderExtremeLoad() {
        for(var quality:ParticleBudget.Quality.values()) {
            var budget=new ParticleBudget();Object level=new Object();int count=0;
            for(int i=0;i<100_000;i++) if(budget.admit(level,7,quality,false)) count++;
            assertEquals(quality.perTick,count);
            assertTrue(quality.liveCap<=1024);assertTrue(quality.distance<=96);
        }
    }
    @Test void resetsOnlyForTheNextTickOrAnotherWorldAndRespectsDecreasedParticles() {
        var budget=new ParticleBudget();Object level=new Object();int count=0;
        for(int i=0;i<400;i++) if(budget.admit(level,1,ParticleBudget.Quality.ULTRA,true)) count++;
        assertEquals(48,count);assertFalse(budget.admit(level,1,ParticleBudget.Quality.ULTRA,true));
        assertTrue(budget.admit(level,2,ParticleBudget.Quality.ULTRA,true));
        assertTrue(budget.admit(new Object(),2,ParticleBudget.Quality.ULTRA,true));
    }
    @Test void lowQualityThinsUniformlyInsteadOfKeepingOnlyTheFirstHalf() {
        var budget=new ParticleBudget();Object level=new Object();
        for(int i=0;i<20;i++) assertEquals(i%2==1,budget.admit(level,0,ParticleBudget.Quality.LOW,false));
    }
}
