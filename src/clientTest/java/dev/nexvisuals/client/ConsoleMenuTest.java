package dev.nexvisuals.client;

import com.google.gson.JsonParser;
import dev.nexvisuals.core.animation.MenuMotion;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.hud.ConsoleLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleMenuTest {
    @TempDir Path directory;

    @Test void colorsMotionAndEnableStateSurviveConfigAndNamedProfile() throws Exception {
        var catalog = new ClientModules();
        var menu = catalog.consoleMenu;
        menu.setEnabled(true);
        menu.alignment.set(ConsoleLayout.Alignment.RIGHT);
        menu.motion.set(MenuMotion.Style.SWAY);
        menu.reverse.set(true);
        menu.speed.set(1.25);
        menu.accent.set(0x807799CC);
        menu.reduceMotion.set(true);
        menu.loading.set(false);
        var globals = new GlobalSettings();
        var config = new ConfigManager(directory.resolve("active.json"), catalog.registry, globals);
        config.save();
        var expected = config.snapshot();
        var restored = new ClientModules();
        var next = new ConfigManager(config.path(), restored.registry, new GlobalSettings());
        assertTrue(next.load().warnings().isEmpty());
        assertEquals(expected, next.snapshot());
        assertTrue(restored.consoleMenu.reduceMotion.get());

        var profiles = new ProfileManager(directory.resolve("profiles"), catalog.registry, globals);
        profiles.save("Моё меню");
        menu.applyPreset(menu.presets().getFirst());
        menu.setEnabled(false);
        assertTrue(profiles.load("Моё меню").warnings().isEmpty());
        assertEquals(expected, config.snapshot());
    }

    @Test void oldConfigStaysOptInAndUnknownMotionFallsBackWithoutLosingValidColor() {
        var catalog = new ClientModules();
        var config = new ConfigManager(directory.resolve("old.json"), catalog.registry, new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("{\"schemaVersion\":1,\"modules\":{}}").getAsJsonObject()).isEmpty());
        assertFalse(catalog.consoleMenu.enabled());
        var warnings = config.apply(JsonParser.parseString("""
                {"schemaVersion":1,"modules":{"console_menu":{"enabled":true,"settings":{
                  "motion":"REMOVED","accent":"#807799CC","speed":999,"future_motion":true
                }}}}
                """).getAsJsonObject());
        assertEquals(1, warnings.size());
        assertEquals(MenuMotion.Style.DRIFT, catalog.consoleMenu.motion.get());
        assertEquals(0x807799CC, catalog.consoleMenu.accent.get());
        assertEquals(3, catalog.consoleMenu.speed.get());
    }

    @Test void classicResetsAQuietPresetWithoutEnablingTheModuleImplicitly() {
        var menu = new ClientModules().consoleMenu;
        menu.applyPreset(menu.presets().stream().filter(p -> p.name().equals("Still")).findFirst().orElseThrow());
        assertTrue(menu.reduceMotion.get());
        menu.applyPreset(menu.presets().stream().filter(p -> p.name().equals("Classic")).findFirst().orElseThrow());
        assertFalse(menu.reduceMotion.get());
        assertFalse(menu.enabled());
        menu.settings().forEach(setting -> assertEquals(setting.defaultValue(), setting.get()));
    }
}
