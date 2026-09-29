package dev.nexvisuals.core.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleCadenceTest {
    @Test void populationIsDistributedWithoutAnInitialBurst() {
        var c=new ParticleCadence(); int emitted=0;
        for(int i=0;i<100;i++) { int count=c.next(40,100,3); assertTrue(count<=1); emitted+=count; }
        assertEquals(40,emitted);
    }
    @Test void overloadCannotBuildAnEmissionBacklog() {
        var c=new ParticleCadence();
        assertEquals(3,c.next(10000,1,3)); assertEquals(0,c.next(0,10,3));
        c.next(5,10,3); c.reset(); assertEquals(0,c.next(5,10,3));
    }
    @Test void invalidLifetimeIsRejected() {
        assertThrows(IllegalArgumentException.class,()->new ParticleCadence().next(5,0,3));
    }
}
