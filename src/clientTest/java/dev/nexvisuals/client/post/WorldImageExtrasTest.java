package dev.nexvisuals.client.post;

import dev.nexvisuals.client.ClientModules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldImageExtrasTest {
    @Test void newPassDoesNotRequireEnablingColorGrading() {
        var c=new ClientModules();var extras=new WorldImageExtras(c.weatherLens,c.underwater,c.retroDisplay);
        assertFalse(extras.requested());assertFalse(extras.active());
        c.retroDisplay.setEnabled(true);
        assertTrue(extras.requested());assertTrue(extras.active());assertFalse(c.post.enabled());
        c.retroDisplay.intensity.set(0.0);
        assertFalse(extras.requested());assertFalse(extras.active());
        c.retroDisplay.setEnabled(false);c.weatherLens.setEnabled(true);
        assertTrue(extras.requested());assertFalse(extras.active(),"Dry rain should not allocate a scene pass");
        c.weatherLens.setEnabled(false);c.underwater.setEnabled(true);
        assertTrue(extras.requested());assertFalse(extras.active(),"Air should not activate the water pass");
        extras.rain=.6f;extras.submerged=.7f;extras.reset();assertFalse(extras.active());
    }
}
