package dev.nexvisuals.client;

import com.google.gson.JsonParser;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.module.VisualModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class SkyVisualsTest {
    @TempDir Path directory;
    @Test void bothCatalogModulesExposeCompleteGroupsAndSevenDistinctPresets() throws Exception {
        var catalog=new ClientModules();
        for(VisualModule module:new VisualModule[]{catalog.skybox,catalog.post}) {
            assertEquals(7,module.presets().size());
            var grouped=new HashSet<String>();
            module.groups().forEach(g -> g.settings().forEach(s -> assertTrue(grouped.add(s.id()))));
            assertEquals(module.settings().size(),grouped.size());
            var snapshots=new HashSet<com.google.gson.JsonObject>();
            var config=new ConfigManager(directory.resolve("active.json"),catalog.registry,new GlobalSettings());
            for(var preset:module.presets()) {
                module.applyPreset(preset);
                assertEquals(preset.name(),module.currentPresetName());
                assertTrue(snapshots.add(config.snapshot()));
            }
        }
    }
    @Test void customStatusConfigAndProfilesTrackManualEditsAfterApplyingAStyle() throws Exception {
        var catalog=new ClientModules();
        var globals=new GlobalSettings();
        catalog.skybox.applyPreset(catalog.skybox.presets().stream().filter(p->p.name().equals("Cyber")).findFirst().orElseThrow());
        catalog.skybox.horizonHeight.set(.21);
        catalog.skybox.moonTint.set(0x809900AA);
        catalog.skybox.starAmount.set(4001);
        catalog.skybox.setEnabled(true);
        catalog.post.applyPreset(catalog.post.presets().stream().filter(p->p.name().equals("Cinematic")).findFirst().orElseThrow());
        catalog.post.temperature.set(.33);
        catalog.post.setEnabled(true);
        assertEquals("Custom",catalog.skybox.currentPresetName()); assertEquals("Custom",catalog.post.currentPresetName());
        var config=new ConfigManager(directory.resolve("active.json"),catalog.registry,globals);
        config.save(); var snapshot=config.snapshot();
        var fresh=new ClientModules();
        var restored=new ConfigManager(config.path(),fresh.registry,new GlobalSettings());
        assertTrue(restored.load().warnings().isEmpty()); assertEquals(snapshot,restored.snapshot());
        assertEquals("Custom",fresh.skybox.currentPresetName()); assertEquals(4000,fresh.skybox.starAmount.get());
        var profiles=new ProfileManager(directory.resolve("profiles"),catalog.registry,globals);
        profiles.save("Cosmic Custom"); profiles.restoreDefaults();
        assertFalse(catalog.skybox.enabled()); assertFalse(catalog.post.enabled());
        assertEquals("Vanilla+",catalog.skybox.currentPresetName());
        assertTrue(profiles.load("Cosmic Custom").warnings().isEmpty()); assertEquals(snapshot,config.snapshot());
    }
    @Test void olderConfigKeepsExistingSkyPaletteAndDefaultsNewModulesSafely() {
        var catalog=new ClientModules();
        var config=new ConfigManager(directory.resolve("old.json"),catalog.registry,new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("""
                {"schemaVersion":1,"modules":{"sky_palette":{"enabled":true,"settings":{"strength":0.4}}}}
                """).getAsJsonObject()).isEmpty());
        assertTrue(catalog.sky.enabled()); assertFalse(catalog.skybox.enabled()); assertFalse(catalog.post.enabled());
        assertEquals("Vanilla+",catalog.post.currentPresetName());
    }
    @Test void malformedEnumsAndNumbersAreIsolatedWhileUnknownFieldsAreIgnored() {
        var catalog=new ClientModules();
        var config=new ConfigManager(directory.resolve("bad-values.json"),catalog.registry,new GlobalSettings());
        var warnings=config.apply(JsonParser.parseString("""
                {"schemaVersion":1,"modules":{
                  "skybox":{"settings":{"stars":"REMOVED","star_amount":-200,"fog_density":0,"future":1}},
                  "post_processing":{"settings":{"gamma":"NaN","brightness":999,"contrast":-1}}
                }}
                """).getAsJsonObject());
        assertEquals(2,warnings.size()); assertEquals(0,catalog.skybox.starAmount.get());
        assertEquals(1,catalog.skybox.fogDensity.get()); assertEquals(1,catalog.post.gamma.get());
        assertEquals(1.4,catalog.post.brightness.get()); assertEquals(.65,catalog.post.contrast.get());
    }
}
