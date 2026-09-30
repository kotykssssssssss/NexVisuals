package dev.nexvisuals.client;

import com.google.gson.JsonParser;
import dev.nexvisuals.client.background.LiveBackgroundModule;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.setting.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the actual menu module and codecs without creating a game, window or GPU device. */
class LiveBackgroundTest {
    @TempDir Path directory;
    @Test void sixRecipesSelectDifferentGeometryAndResetToAnEditableStartingPoint() {
        var live=new ClientModules().liveBackground;
        var styles=new HashSet<LiveBackgroundModule.Style>();
        for (var preset : live.presets()) {
            live.applyPreset(preset);
            assertTrue(styles.add(live.style.get()));
            assertEquals(preset.name(),live.currentPresetName());
            assertEquals(LiveBackgroundModule.Background.LIVE,live.background.get());
            live.speed.set(1.7);
            assertEquals("Custom",live.currentPresetName());
        }
        assertEquals(6,styles.size());
        live.settings().forEach(Setting::reset);
        assertEquals("NexVisuals",live.currentPresetName());
    }
    @Test void olderMenuConfigKeepsLayoutMotionAndLeavesLiveBackgroundOff() {
        var catalog=new ClientModules();
        var config=new ConfigManager(directory.resolve("legacy.json"),catalog.registry,new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("""
                {"schemaVersion":1,"modules":{"console_menu":{"enabled":true,
                  "settings":{"speed":0.4,"reduce_motion":true}}}}
                """).getAsJsonObject()).isEmpty());
        assertTrue(catalog.consoleMenu.enabled()); assertTrue(catalog.consoleMenu.reduceMotion.get());
        assertEquals(.4,catalog.consoleMenu.speed.get());
        assertFalse(catalog.liveBackground.enabled());
        assertFalse(catalog.liveBackground.pauseMenu.get());
    }
    @Test void malformedStyleIsIsolatedAndMotionParametersStayWithinGpuBounds() {
        var catalog=new ClientModules();
        var config=new ConfigManager(directory.resolve("bounds.json"),catalog.registry,new GlobalSettings());
        var warnings=config.apply(JsonParser.parseString("""
                {"schemaVersion":1,"modules":{
                  "console_menu":{"enabled":true},
                  "live_background":{"settings":{"style":"REMOVED","speed":999,"particles":999,
                    "motion":-5,"softness":0,"dim":10,"brightness":-1,"future":true}},
                  "post_processing":{"settings":{"exposure":99,"highlights":-1,"grain_scale":0}}
                }}
                """).getAsJsonObject());
        assertEquals(1,warnings.size()); assertTrue(catalog.consoleMenu.enabled());
        var live=catalog.liveBackground;
        assertEquals(LiveBackgroundModule.Style.NEXVISUALS,live.style.get());
        assertEquals(2,live.speed.get()); assertEquals(32,live.particles.get());
        assertEquals(0,live.motion.get()); assertEquals(.1,live.softness.get());
        assertEquals(.8,live.dim.get()); assertEquals(.3,live.brightness.get());
        assertEquals(.65,catalog.post.exposure.get()); assertEquals(0,catalog.post.highlights.get());
        assertEquals(.5,catalog.post.grainScale.get());
    }
    @Test void vanillaFallbackIsIndependentOfColorsAndConsoleLayoutAndSurvivesReload() throws Exception {
        var catalog=new ClientModules();
        catalog.consoleMenu.setEnabled(true);
        var live=catalog.liveBackground;
        live.setEnabled(true); live.background.set(LiveBackgroundModule.Background.VANILLA);
        live.primary.set(0x809BC08F); live.speed.set(0.0);
        var config=new ConfigManager(directory.resolve("fallback.json"),catalog.registry,new GlobalSettings());
        config.save();
        var fresh=new ClientModules();
        assertTrue(new ConfigManager(config.path(),fresh.registry,new GlobalSettings()).load().warnings().isEmpty());
        assertTrue(fresh.consoleMenu.enabled()); assertTrue(fresh.liveBackground.enabled());
        assertEquals(LiveBackgroundModule.Background.VANILLA,fresh.liveBackground.background.get());
        assertEquals(0x809BC08F,fresh.liveBackground.primary.get()); assertEquals(0,fresh.liveBackground.speed.get());
        fresh.liveBackground.background.set(LiveBackgroundModule.Background.LIVE);
        assertEquals(0x809BC08F,fresh.liveBackground.primary.get());
    }
}
