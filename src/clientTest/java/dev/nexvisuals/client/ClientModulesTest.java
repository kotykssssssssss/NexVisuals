package dev.nexvisuals.client;

import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.setting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

/** No Minecraft instance or windows; exercises the real catalog used by the entrypoint. */
class ClientModulesTest {
    @TempDir Path directory;
    @Test void realCatalogHasUniqueModulesAndSettingIdsAndStartsDisabled() {
        ClientModules c = new ClientModules();
        var ids = new HashSet<String>();
        for (var module : c.registry.all()) {
            assertTrue(ids.add(module.id())); assertFalse(module.enabled());
            var settingIds = new HashSet<String>();
            for (var s : module.settings()) assertTrue(settingIds.add(s.id()), module.id()+"/"+s.id());
        }
        assertTrue(ids.containsAll(java.util.Set.of("viewmodel", "swing", "shield", "fire_overlay", "hit_visuals", "player_trails", "cosmetic_hat", "container_visuals", "fireflies", "elytra_trails")));
    }
    @Test void everyBuiltInPresetCanBeAppliedAndSavedWithoutValidationWarnings() throws Exception {
        ClientModules c = new ClientModules();
        for (var module : c.registry.all()) for (var preset : module.presets()) {
            assertDoesNotThrow(()->module.applyPreset(preset), module.id()+"/"+preset.name());
        }
        c.registry.all().forEach(m -> m.setEnabled(true));
        var globals = new GlobalSettings();
        ConfigManager config = new ConfigManager(directory.resolve("config.json"), c.registry, globals);
        config.save(); var snapshot = config.snapshot();
        ClientModules restored = new ClientModules();
        ConfigManager next = new ConfigManager(config.path(), restored.registry, new GlobalSettings());
        assertTrue(next.load().warnings().isEmpty()); assertEquals(snapshot, next.snapshot());
        next.resetDefaults();
        for (var m : restored.registry.all()) {
            assertFalse(m.enabled());
            for (var s : m.settings()) assertEquals(s.defaultValue(), s.get());
        }
    }
    @Test void deepHandTransformsCopyAndMirrorWithoutChangingMainHand() {
        var module = new ClientModules().viewmodel;
        var mainX = (DoubleSetting) module.settings().stream().filter(s->s.id().equals("main_x")).findFirst().orElseThrow();
        mainX.set(.4);
        module.actions().stream().filter(a->a.name().startsWith("Mirror")).findFirst().orElseThrow().run().run();
        var offX = (DoubleSetting) module.settings().stream().filter(s->s.id().equals("off_x")).findFirst().orElseThrow();
        assertEquals(-.4, offX.get()); assertEquals(.4, mainX.get());
        assertTrue((Boolean) module.settings().stream().filter(s->s.id().equals("separate_offhand")).findFirst().orElseThrow().get());
    }
    @Test void foundationConfigKeepsExistingIdsAndDefaultsNewSettings() {
        ClientModules c = new ClientModules();
        ConfigManager config = new ConfigManager(directory.resolve("old.json"), c.registry, new GlobalSettings());
        var old = com.google.gson.JsonParser.parseString("""
                {"schemaVersion":1,"modules":{
                  "custom_crosshair":{"enabled":true,"settings":{"size":12,"color":"#DDAABBCC"}},
                  "hud_coordinates":{"enabled":true,"settings":{"x":48,"y":96}},
                  "viewmodel":{"settings":{"main_scale":0.75,"main_x":0.2}}
                }}
                """).getAsJsonObject();
        assertTrue(config.apply(old).isEmpty());
        assertEquals(12, c.registry.find("custom_crosshair").orElseThrow().settings().stream().filter(s->s.id().equals("size")).findFirst().orElseThrow().get());
        assertEquals(48, c.registry.find("hud_coordinates").orElseThrow().settings().stream().filter(s->s.id().equals("x")).findFirst().orElseThrow().get());
        assertEquals(1.0, c.viewmodel.settings().stream().filter(s->s.id().equals("main_scale_x")).findFirst().orElseThrow().get());
    }
    @Test void bundledGlobalStylesReferenceExistingSettingsAndRemainDistinct() throws Exception {
        ClientModules catalog = new ClientModules();
        GlobalSettings globals = new GlobalSettings();
        ProfileManager profiles = new ProfileManager(directory, catalog.registry, globals);
        ConfigManager snapshotter = new ConfigManager(directory.resolve("active.json"), catalog.registry, globals);
        var snapshots = new HashSet<com.google.gson.JsonObject>();
        for (BuiltinProfile preset : BuiltinProfile.values()) {
            String resource = "/nexvisuals/presets/"+preset.name().toLowerCase(java.util.Locale.ROOT)+".json";
            try (var input = getClass().getResourceAsStream(resource)) {
                assertNotNull(input);
                var root = com.google.gson.JsonParser.parseString(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (var entry : root.getAsJsonObject("modules").entrySet()) {
                    var module = catalog.registry.find(entry.getKey()).orElseThrow();
                    for (var key : entry.getValue().getAsJsonObject().getAsJsonObject("settings").keySet())
                        assertTrue(module.settings().stream().anyMatch(s->s.id().equals(key)), module.id()+"/"+key);
                }
            }
            assertTrue(profiles.applyBuiltin(preset).isEmpty()); assertTrue(snapshots.add(snapshotter.snapshot()));
            profiles.save(preset.label());
            assertTrue(profiles.load(preset.label()).warnings().isEmpty());
        }
        assertEquals(3,profiles.list().size());
    }
}
