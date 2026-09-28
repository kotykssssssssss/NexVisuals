package dev.nexvisuals.core.config;

import dev.nexvisuals.core.TestModule;
import dev.nexvisuals.core.module.ModuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ProfileManagerTest {
    @TempDir Path directory;

    @Test void multipleProfilesCanBeSavedListedLoadedUpdatedAndDeleted() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        ProfileManager profiles = new ProfileManager(directory.resolve("profiles"), registry, new GlobalSettings());
        assertTrue(profiles.list().isEmpty());
        module.count.set(8);
        module.setEnabled(true);
        profiles.save("pvp_visuals");
        module.count.set(2);
        module.setEnabled(false);
        profiles.save("minimal");
        assertEquals(java.util.List.of("minimal", "pvp_visuals"), profiles.list());
        assertEquals(ConfigManager.LoadStatus.LOADED, profiles.load("pvp_visuals").status());
        assertEquals(8, module.count.get());
        assertTrue(module.enabled());
        module.count.set(9);
        profiles.save("pvp_visuals");
        profiles.load("minimal");
        assertEquals(2, module.count.get());
        assertFalse(module.enabled());
        profiles.load("pvp_visuals");
        assertEquals(9, module.count.get());
        assertTrue(profiles.delete("minimal"));
        assertFalse(profiles.delete("minimal"));
        assertEquals(java.util.List.of("pvp_visuals"), profiles.list());
        profiles.restoreDefaults();
        assertEquals(4, module.count.get());
        assertFalse(module.enabled());
    }

    @Test void profilesPersistAcrossManagerAndModuleInstances() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new TestModule("test")).text.set("Profile");
        new ProfileManager(directory, registry, new GlobalSettings()).save("saved");
        ModuleRegistry restoredRegistry = new ModuleRegistry();
        TestModule restored = restoredRegistry.register(new TestModule("test"));
        new ProfileManager(directory, restoredRegistry, new GlobalSettings()).load("saved");
        assertEquals("Profile", restored.text.get());
    }

    @Test void corruptAndMissingProfilesPreserveCurrentValues() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        TestModule module = registry.register(new TestModule("test"));
        module.count.set(9);
        module.setEnabled(true);
        ProfileManager profiles = new ProfileManager(directory, registry, new GlobalSettings());
        Files.writeString(directory.resolve("broken.json"), "{broken");
        ConfigManager.LoadResult result = profiles.load("broken");
        assertEquals(ConfigManager.LoadStatus.RECOVERED, result.status());
        assertEquals("{broken", Files.readString(result.backup()));
        assertEquals(9, module.count.get());
        assertTrue(module.enabled());
        assertThrows(IOException.class, () -> profiles.load("missing"));
        assertEquals(9, module.count.get());
        assertEquals(java.util.List.of("broken"), profiles.list());
    }

    @Test void traversalAbsolutePathsAndDeviceNamesAreRejectedBeforeWriting() {
        ProfileManager profiles = new ProfileManager(directory, new ModuleRegistry(), new GlobalSettings());
        for (String name : new String[]{"../outside", "..", "a/b", "a\\b", "C:\\bad", "/bad", "", "nul", "con", "com1", "lpt9", "with space", "UPPER"}) {
            assertFalse(ProfileManager.validName(name), name);
            assertThrows(IllegalArgumentException.class, () -> profiles.save(name), name);
            assertThrows(IllegalArgumentException.class, () -> profiles.delete(name), name);
        }
        assertTrue(ProfileManager.validName("visuals-01"));
        assertFalse(ProfileManager.validName("x".repeat(49)));
    }
}
