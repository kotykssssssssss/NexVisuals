package dev.nexvisuals.client;

import dev.nexvisuals.client.hud.VanillaHudModule;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.setting.*;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MiniHudTest {
    private static Setting<?> setting(VanillaHudModule module, String id) {
        return module.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();
    }
    @Test void miniPresetScalesDisabledCompanionLayersWithoutEnablingTheirCustomization() {
        var catalog=new ClientModules(); var hotbar=catalog.hotbar;
        hotbar.applyPreset(hotbar.presets().stream().filter(p->p.name().equals("Mini")).findFirst().orElseThrow()); hotbar.setEnabled(true);
        for (String id : new String[]{"health","armor","hunger","experience","level"}) {
            var module=(VanillaHudModule)catalog.registry.find("vanilla_"+id).orElseThrow();
            assertFalse(module.enabled()); assertTrue(module.isHudActive(false)); assertFalse(module.isVisibleInEditor(null));
            assertEquals(hotbar.transform(960,540),module.transform(960,540));
        }
        var boss=(VanillaHudModule)catalog.registry.find("vanilla_boss").orElseThrow();
        assertFalse(boss.isHudActive(false)); assertTrue(boss.isVisibleInEditor(null));
    }
    @Test void disablingGroupOrLinkRestoresIndependentSettingsWithoutDeletingThem() {
        var catalog=new ClientModules(); var hotbar=catalog.hotbar;
        var health=(VanillaHudModule)catalog.registry.find("vanilla_health").orElseThrow();
        ((DoubleSetting)setting(health,"scale")).set(1.4); hotbar.setEnabled(true);
        assertEquals(1,health.transform(960,540).scale());
        ((BooleanSetting)setting(hotbar,"linked")).set(false);
        assertEquals(1.4,health.transform(960,540).scale()); assertFalse(health.isHudActive(false));
        ((BooleanSetting)setting(hotbar,"linked")).set(true); hotbar.setEnabled(false);
        assertFalse(health.isHudActive(false)); assertEquals(1.4,health.transform(960,540).scale());
    }
    @Test void previousMiniHudConfigGetsOneTransformAndKeepsItsScaleAndColors() {
        var catalog=new ClientModules();
        var config=new ConfigManager(Path.of("unused.json"),catalog.registry,new GlobalSettings());
        var old=JsonParser.parseString("""
            {"schemaVersion":1,"modules":{
              "vanilla_hotbar":{"enabled":true,"settings":{"scale":0.8,"accent":"#FFAABBCC"}},
              "vanilla_health":{"enabled":true,"settings":{"scale":0.8}},
              "vanilla_hunger":{"enabled":true,"settings":{"scale":1.0}}
            }}
            """).getAsJsonObject();
        assertTrue(config.apply(old).isEmpty()); assertTrue(catalog.hotbar.linkedLayout());
        var food=(VanillaHudModule)catalog.registry.find("vanilla_hunger").orElseThrow();
        assertEquals(.8,food.transform(960,540).scale()); assertEquals(0xFFAABBCC,setting(catalog.hotbar,"accent").get());
        ((BooleanSetting)setting(catalog.hotbar,"linked")).set(false);
        assertEquals(1,food.transform(960,540).scale());
    }
    @Test void draggingGroupAndResizingKeepsOneCommonTransform() {
        var catalog=new ClientModules(); var hotbar=catalog.hotbar;
        hotbar.setEnabled(true); ((DoubleSetting)setting(hotbar,"scale")).set(.8);
        hotbar.moveTo(120,80,960,540);
        var bounds=hotbar.bounds(null,960,540);
        assertEquals(120,bounds.x()); assertEquals(80,bounds.y());
        for(int width : new int[]{480,960,1280}) {
            int height=width*9/16;
            var resized=hotbar.bounds(null,width,height);
            assertTrue(resized.x()>=0 && resized.x()+resized.width()<=width);
            assertTrue(resized.y()>=0 && resized.y()+resized.height()<=height);
            var xp=(VanillaHudModule)catalog.registry.find("vanilla_experience").orElseThrow();
            assertEquals(hotbar.transform(width,height),xp.transform(width,height));
        }
    }
}
