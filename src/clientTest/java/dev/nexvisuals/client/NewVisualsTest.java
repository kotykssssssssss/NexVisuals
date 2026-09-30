package dev.nexvisuals.client;

import com.google.gson.*;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses real catalog/config/profile codecs; no client singleton, window or GPU initialization. */
class NewVisualsTest {
    @TempDir Path directory;
    private List<VisualModule> additions(ClientModules c) {
        return List.of(c.jumpRings,c.footsteps,c.totemEcho,c.blockEffects,c.orbitals,c.weatherLens,c.underwater,c.retroDisplay);
    }
    @Test void eightIndependentModulesHaveDistinctRecipesAndEditableCustomState() {
        var c=new ClientModules();
        var config=new ConfigManager(directory.resolve("active.json"),c.registry,new GlobalSettings());
        assertEquals(8,additions(c).size());
        for(var module:additions(c)) {
            assertFalse(module.enabled());assertTrue(module.presets().size()>=3);
            var snapshots=new HashSet<JsonObject>();
            for(var preset:module.presets()) {
                module.applyPreset(preset);
                assertEquals(preset.name(),module.currentPresetName());
                assertTrue(snapshots.add(config.snapshot()),module.id()+" duplicate recipe");
                var color=module.settings().stream().filter(s->s instanceof ColorSetting).findFirst();
                if(color.isPresent()) ((ColorSetting)color.get()).set(0x66123456);
                else ((DoubleSetting)module.settings().stream().filter(s->s.id().equals("intensity")).findFirst().orElseThrow()).set(.12345);
                assertEquals("Custom",module.currentPresetName());
            }
        }
    }
    @Test void nondefaultNewSettingsSurviveConfigRestartAndProfileRenameLoad() throws Exception {
        var c=new ClientModules();var globals=new GlobalSettings();
        for(var m:additions(c)) {m.applyPreset(m.presets().get(1));m.setEnabled(true);}
        c.weatherLens.refraction.set(3.25);c.underwater.wobble.set(2.75);c.retroDisplay.pixelSize.set(7);
        var config=new ConfigManager(directory.resolve("active.json"),c.registry,globals);config.save();var expected=config.snapshot();
        var fresh=new ClientModules();var restored=new ConfigManager(config.path(),fresh.registry,new GlobalSettings());
        assertTrue(restored.load().warnings().isEmpty());assertEquals(expected,restored.snapshot());
        for(var m:additions(fresh)) assertTrue(m.enabled());
        var profiles=new ProfileManager(directory.resolve("profiles"),c.registry,globals);
        profiles.save("New Visuals");profiles.rename("New Visuals","Eight Styles");profiles.restoreDefaults();
        assertTrue(additions(c).stream().noneMatch(VisualModule::enabled));
        assertTrue(profiles.load("Eight Styles").warnings().isEmpty());assertEquals(expected,config.snapshot());
        assertTrue(profiles.delete("Eight Styles"));assertTrue(profiles.list().isEmpty());
    }
    @Test void legacyConfigRetainsNightSkyAndDefaultsAllEightAdditionsOff() {
        var c=new ClientModules();var config=new ConfigManager(directory.resolve("old.json"),c.registry,new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("""
            {"schemaVersion":1,"modules":{
              "skybox":{"enabled":true,"settings":{"night_sky":"#FF241344","nebula":true}},
              "post_processing":{"enabled":true,"settings":{"bloom":true,"glow_footprint":"WIDE"}},
              "shield":{"enabled":true},"fire_overlay":{"enabled":true}}}
            """).getAsJsonObject()).isEmpty());
        assertEquals(0xFF241344,c.skybox.nightSky.get());assertTrue(c.shield.enabled());assertTrue(c.fire.enabled());
        for(var m:additions(c)) {assertFalse(m.enabled());m.settings().forEach(s->assertEquals(s.defaultValue(),s.get()));}
    }
    @Test void malformedNewEnumsAreIsolatedAndParticleGpuBoundsAreClamped() {
        var c=new ClientModules();var config=new ConfigManager(directory.resolve("bad.json"),c.registry,new GlobalSettings());
        var warnings=config.apply(JsonParser.parseString("""
            {"schemaVersion":1,"modules":{
              "jump_rings":{"settings":{"style":"REMOVED","radius":100}},
              "totem_echo":{"settings":{"amount":1000000}},
              "cosmetic_orbitals":{"settings":{"count":999}},
              "weather_lens":{"settings":{"style":"INVALID","density":100,"refraction":999}},
              "underwater_effects":{"settings":{"wobble":999,"caustics":999,"transition":0}},
              "retro_display":{"settings":{"pixel_size":0,"levels":0,"scanlines":99,"future":true}}}}
            """).getAsJsonObject());
        assertEquals(2,warnings.size());assertEquals(20,c.weatherLens.density.get());assertEquals(5,c.weatherLens.refraction.get());
        assertEquals(5,c.underwater.wobble.get());assertEquals(.25,c.underwater.caustics.get());assertEquals(.1,c.underwater.transition.get());
        assertEquals(1,c.retroDisplay.pixelSize.get());assertEquals(4,c.retroDisplay.levels.get());assertEquals(.4,c.retroDisplay.scanlines.get());
        assertEquals(96,setting(c.totemEcho,"amount").get());assertEquals(12,setting(c.orbitals,"count").get());assertEquals(3.0,setting(c.jumpRings,"radius").get());
    }
    private Setting<?> setting(VisualModule m,String id) { return m.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(); }
    @Test void corruptNewProfileIsBackedUpWithoutOverwritingActiveVisuals() throws Exception {
        var c=new ClientModules();var globals=new GlobalSettings();c.retroDisplay.setEnabled(true);c.retroDisplay.pixelSize.set(5);
        var config=new ConfigManager(directory.resolve("active.json"),c.registry,globals);var expected=config.snapshot();
        var profiles=new ProfileManager(directory.resolve("profiles"),c.registry,globals);profiles.save("Broken");
        Files.writeString(profiles.directory().resolve("Broken.json"),"{\"modules\":");
        var result=profiles.load("Broken");assertNotNull(result.backup());assertTrue(Files.exists(result.backup()));
        assertEquals(expected,config.snapshot());
    }
}
