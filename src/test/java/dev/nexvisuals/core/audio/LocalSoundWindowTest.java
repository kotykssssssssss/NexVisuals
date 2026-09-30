package dev.nexvisuals.core.audio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalSoundWindowTest {
    @Test void windowRequiresRecentActionSameWorldAndNearbyOrigin() {
        var window=new LocalSoundWindow();Object world=new Object();long now=1_000_000_000L;
        assertFalse(window.contains(now,world,3,4,5));
        window.arm(now,world,3,4,5);
        assertTrue(window.contains(now+450_000_000L,world,4,4,5));
        assertFalse(window.contains(now+450_000_000L,world,4.01,4,5));
        assertFalse(window.contains(now+451_000_000L,world,3,4,5));
        window.arm(now,world,3,4,5);assertFalse(window.contains(now,new Object(),3,4,5));
        assertFalse(window.contains(now,world,3,4,5));
        window.arm(now,world,3,4,5);assertFalse(window.contains(now-1,world,3,4,5));
        window.arm(now,world,3,4,5);window.clear();assertFalse(window.contains(now,world,3,4,5));
    }
    @Test void onlyTheSixVanillaAttackEventsAreEligibleNotHurtDeathTotemOrCustomAudio() {
        for(String suffix:new String[]{"strong","weak","crit","knockback","sweep","nodamage"})
            assertTrue(LocalSoundWindow.isVanillaAttack("minecraft","entity.player.attack."+suffix));
        for(String path:new String[]{"entity.player.hurt","entity.player.death","entity.zombie.hurt","item.totem.use","entity.player.attack.future","ui.button.click"})
            assertFalse(LocalSoundWindow.isVanillaAttack("minecraft",path));
        assertFalse(LocalSoundWindow.isVanillaAttack("othermod","entity.player.attack.crit"));
        assertFalse(LocalSoundWindow.isVanillaAttack("nexvisuals","user_hit_critical"));
    }
}
