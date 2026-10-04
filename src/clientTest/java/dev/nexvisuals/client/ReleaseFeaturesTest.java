package dev.nexvisuals.client;

import com.google.gson.JsonParser;
import dev.nexvisuals.client.hud.EditableHud;
import dev.nexvisuals.core.config.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseFeaturesTest {
    @TempDir Path directory;

    @Test void newModulesHaveDifferentPresetsAndUseTheExistingHudAndConfigContracts() {
        var catalog = new ClientModules();
        assertFalse(catalog.projectileTrails.enabled()); assertFalse(catalog.pickups.enabled());
        assertInstanceOf(EditableHud.class, catalog.pickups);
        var config = new ConfigManager(directory.resolve("active.json"), catalog.registry, new GlobalSettings());
        for (var module : List.of(catalog.projectileTrails, catalog.pickups)) {
            var recipes = new HashSet<com.google.gson.JsonObject>();
            for (var preset : module.presets()) {
                module.applyPreset(preset);
                assertEquals(preset.name(), module.currentPresetName());
                assertTrue(recipes.add(config.snapshot()));
            }
            assertEquals(module.presets().size(), recipes.size());
        }
    }
    @Test void newSettingsSurviveConfigRestartProfilesAndDefaultReset() throws Exception {
        var catalog = new ClientModules(); var globals = new GlobalSettings();
        catalog.projectileTrails.applyPreset(catalog.projectileTrails.presets().get(2)); catalog.projectileTrails.setEnabled(true);
        catalog.pickups.applyPreset(catalog.pickups.presets().get(1)); catalog.pickups.setEnabled(true);
        var config = new ConfigManager(directory.resolve("active.json"), catalog.registry, globals);
        config.save(); var expected = config.snapshot();
        var next = new ClientModules(); var nextGlobals = new GlobalSettings();
        var restored = new ConfigManager(config.path(), next.registry, nextGlobals);
        assertTrue(restored.load().warnings().isEmpty()); assertEquals(expected, restored.snapshot());
        var profiles = new ProfileManager(directory.resolve("profiles"), next.registry, nextGlobals);
        profiles.save("Release Setup"); restored.resetDefaults();
        assertFalse(next.projectileTrails.enabled()); assertFalse(next.pickups.enabled());
        assertTrue(profiles.load("Release Setup").warnings().isEmpty()); assertEquals(expected, restored.snapshot());
    }
    @Test void oldConfigsPreserveExistingVisualsAndInvalidNewValuesAreIsolatedAndClamped() {
        var catalog = new ClientModules();
        var config = new ConfigManager(directory.resolve("old.json"), catalog.registry, new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("""
                {"schemaVersion":1,"modules":{"viewmodel":{"enabled":true,"settings":{"main_scale":0.9}},
                "fire_overlay":{"enabled":true},"skybox":{"enabled":true,"settings":{"nebula":true}}}}
                """).getAsJsonObject()).isEmpty());
        assertTrue(catalog.viewmodel.enabled()); assertTrue(catalog.fire.enabled()); assertTrue(catalog.skybox.nebula.get());
        assertFalse(catalog.projectileTrails.enabled()); assertFalse(catalog.pickups.enabled());
        var warnings = config.apply(JsonParser.parseString("""
                {"schemaVersion":1,"modules":{"projectile_trails":{"settings":{"style":"REMOVED","density":999,"lifetime":999}},
                "hud_pickups":{"settings":{"rows":999,"duration":999}}}}
                """).getAsJsonObject());
        assertEquals(1, warnings.size());
        assertEquals(12, setting(catalog.projectileTrails, "density").get());
        assertEquals(40, setting(catalog.projectileTrails, "lifetime").get());
        assertEquals(6, setting(catalog.pickups, "rows").get());
        assertEquals(8.0, setting(catalog.pickups, "duration").get());
    }
    @Test void disabledCallbacksDoNotAccessMinecraftOrRetainWorldObjects() {
        var catalog = new ClientModules();
        assertDoesNotThrow(() -> catalog.projectileTrails.tick(null));
        assertDoesNotThrow(() -> catalog.projectileTrails.loaded(null, null));
        assertDoesNotThrow(() -> catalog.pickups.pickedUp(null, null, 1));
    }
    private dev.nexvisuals.core.setting.Setting<?> setting(dev.nexvisuals.core.module.VisualModule module, String id) {
        return module.settings().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
}
