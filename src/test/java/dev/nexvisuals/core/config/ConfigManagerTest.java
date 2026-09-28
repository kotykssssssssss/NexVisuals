package dev.nexvisuals.core.config;

import dev.nexvisuals.core.TestModule;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.setting.DoubleRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {
    @TempDir Path directory;

    @Test void missingConfigurationUsesDefaultsWithoutCreatingFiles() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        ConfigManager config = new ConfigManager(directory.resolve("nexvisuals.json"), registry, new GlobalSettings());
        assertEquals(ConfigManager.LoadStatus.MISSING, config.load().status());
        assertFalse(module.enabled());
        assertEquals(4, module.count.get());
        assertFalse(Files.exists(config.path()));
    }

    @Test void allSettingTypesAndGlobalStateSurviveFreshInstances() throws Exception {
        Path path = directory.resolve("nested/nexvisuals.json");
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        GlobalSettings globals = new GlobalSettings();
        module.setEnabled(true);
        module.flag.set(false);
        module.count.set(9);
        module.size.set(2.5);
        module.range.set(new DoubleRange(1, 7));
        module.style.set(TestModule.Style.COMPACT);
        module.color.set(0x80336699);
        module.text.set("Custom");
        module.key.set("key.mouse.left");
        globals.animationDuration.set(320);
        globals.setSelectedCategory(Category.HUD);
        globals.setSelectedModule("test");
        new ConfigManager(path, registry, globals).save();

        ModuleRegistry nextRegistry = new ModuleRegistry();
        TestModule restored = nextRegistry.register(new TestModule("test"));
        GlobalSettings nextGlobals = new GlobalSettings();
        ConfigManager.LoadResult result = new ConfigManager(path, nextRegistry, nextGlobals).load();
        assertEquals(ConfigManager.LoadStatus.LOADED, result.status());
        assertTrue(result.warnings().isEmpty());
        assertTrue(restored.enabled());
        assertFalse(restored.flag.get());
        assertEquals(9, restored.count.get());
        assertEquals(2.5, restored.size.get());
        assertEquals(new DoubleRange(1, 7), restored.range.get());
        assertEquals(TestModule.Style.COMPACT, restored.style.get());
        assertEquals(0x80336699, restored.color.get());
        assertEquals("Custom", restored.text.get());
        assertEquals("key.mouse.left", restored.key.get());
        assertEquals(320, nextGlobals.animationDuration.get());
        assertEquals("test", nextGlobals.selectedModule());
        try (var files = Files.list(path.getParent())) { assertEquals(1, files.count()); }
    }

    @Test void malformedConfigIsBackedUpExactlyBeforeItCanBeReplaced() throws Exception {
        Path path = directory.resolve("nexvisuals.json");
        String broken = "{\"modules\": [ definitely broken";
        Files.writeString(path, broken);
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        module.count.set(10);
        ConfigManager config = new ConfigManager(path, registry, new GlobalSettings());
        ConfigManager.LoadResult result = config.load();
        assertEquals(ConfigManager.LoadStatus.RECOVERED, result.status());
        assertEquals(broken, Files.readString(result.backup()));
        assertEquals(broken, Files.readString(path));
        assertEquals(4, module.count.get());
        config.save();
        assertEquals(broken, Files.readString(result.backup()));
        assertEquals(ConfigManager.LoadStatus.LOADED, config.load().status());
    }

    @Test void unknownFieldsAreIgnoredMissingFieldsDefaultAndBadValuesAreIsolated() throws Exception {
        Path path = directory.resolve("nexvisuals.json");
        Files.writeString(path, """
                {"schemaVersion":1,"unknown":"future","modules":{
                  "retired_module":{"enabled":true},
                  "test":{"enabled":true,"settings":{"count":999,"size":"wrong","style":"REMOVED","future":1}}
                }}
                """);
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        ConfigManager.LoadResult result = new ConfigManager(path, registry, new GlobalSettings()).load();
        assertTrue(module.enabled());
        assertEquals(10, module.count.get());
        assertEquals(1.0, module.size.get());
        assertEquals(TestModule.Style.SIMPLE, module.style.get());
        assertTrue(module.flag.get());
        assertEquals(2, result.warnings().size());
    }

    @Test void invalidUtf8IsBackedUpWithoutChangingItsBytes() throws Exception {
        Path path = directory.resolve("nexvisuals.json");
        byte[] malformed = {(byte) 0xC3, (byte) 0x28};
        Files.write(path, malformed);
        ConfigManager config = new ConfigManager(path, new ModuleRegistry(), new GlobalSettings());
        ConfigManager.LoadResult result = config.load();
        assertEquals(ConfigManager.LoadStatus.RECOVERED, result.status());
        assertArrayEquals(malformed, Files.readAllBytes(result.backup()));
    }

    @Test void oldSchemaLoadsKnownFieldsAndUsefulDefaults() throws Exception {
        Path path = directory.resolve("nexvisuals.json");
        Files.writeString(path, "{\"schemaVersion\":0,\"modules\":{\"test\":{\"settings\":{\"count\":7}}}}");
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        ConfigManager.LoadResult result = new ConfigManager(path, registry, new GlobalSettings()).load();
        assertEquals(7, module.count.get());
        assertEquals(1, result.warnings().size());
        assertFalse(module.enabled());
    }

    @Test void rememberSelectionCanBeDisabledAndDefaultsCanBeRestored() throws Exception {
        Path path = directory.resolve("nexvisuals.json");
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        GlobalSettings globals = new GlobalSettings();
        ConfigManager config = new ConfigManager(path, registry, globals);
        globals.rememberGui.set(false);
        globals.setSelectedModule("test");
        module.count.set(9);
        module.setEnabled(true);
        config.save();
        assertFalse(Files.readString(path).contains("\"gui\""));
        config.load();
        assertEquals("", globals.selectedModule());
        config.resetDefaults();
        assertEquals(4, module.count.get());
        assertFalse(module.enabled());
        assertTrue(globals.rememberGui.get());
    }
}
