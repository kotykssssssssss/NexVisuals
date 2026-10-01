package dev.nexvisuals.client;

import com.google.gson.JsonParser;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.setting.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TrailAndTotemTest {
    @TempDir Path directory;
    @Test void newStylesAndTransformsRoundTripThroughConfigAndProfiles() throws Exception {
        ClientModules catalog=new ClientModules();
        catalog.trails.applyPreset(catalog.trails.presets().stream().filter(p->p.name().equals("Twin Flow")).findFirst().orElseThrow());
        catalog.weaponTrails.applyPreset(catalog.weaponTrails.presets().get(1));
        catalog.swing.applyPreset(catalog.swing.presets().stream().filter(p->p.name().equals("SWIPE")).findFirst().orElseThrow());
        ((DoubleSetting)catalog.weaponTrails.settings().stream().filter(s->s.id().equals("coverage")).findFirst().orElseThrow()).set(.4);
        catalog.totemAnimation.applyPreset(catalog.totemAnimation.presets().stream().filter(p->p.name().equals("Spin")).findFirst().orElseThrow());
        catalog.trails.setEnabled(true);catalog.weaponTrails.setEnabled(true);catalog.totemAnimation.setEnabled(true);catalog.swing.setEnabled(true);
        GlobalSettings globals=new GlobalSettings();
        ConfigManager config=new ConfigManager(directory.resolve("active.json"),catalog.registry,globals);
        config.save();var snapshot=config.snapshot();
        ProfileManager profiles=new ProfileManager(directory.resolve("profiles"),catalog.registry,globals);
        profiles.save("My ribbons");config.resetDefaults();assertFalse(catalog.trails.enabled());
        assertTrue(profiles.load("My ribbons").warnings().isEmpty());assertEquals(snapshot,config.snapshot());
        ClientModules restored=new ClientModules();ConfigManager next=new ConfigManager(config.path(),restored.registry,new GlobalSettings());
        assertTrue(next.load().warnings().isEmpty());assertEquals(snapshot,next.snapshot());
    }
    @Test void legacyParticleModesAndMissingNewSettingsRemainSafe() {
        for(String mode:new String[]{"MOTES","SILK","SPARKS","RINGS","RAINBOW"}) {
            ClientModules catalog=new ClientModules();ConfigManager config=new ConfigManager(directory.resolve("old.json"),catalog.registry,new GlobalSettings());
            var old=JsonParser.parseString("{\"schemaVersion\":1,\"modules\":{\"player_trails\":{\"enabled\":true,\"settings\":{\"style\":\""+mode+"\",\"size\":0.16}}}}").getAsJsonObject();
            assertTrue(config.apply(old).isEmpty());
            assertEquals(mode,catalog.trails.settings().stream().filter(s->s.id().equals("style")).findFirst().orElseThrow().get().toString());
            assertFalse(catalog.weaponTrails.enabled());assertFalse(catalog.totemAnimation.enabled());
            assertEquals(6.0,catalog.trails.settings().stream().filter(s->s.id().equals("length")).findFirst().orElseThrow().get());
        }
    }
    @Test void customAlignmentAndTotemPlacementClampAndPresetEditsBecomeCustom() {
        ClientModules catalog=new ClientModules();
        var base=(DoubleSetting)catalog.weaponTrails.settings().stream().filter(s->s.id().equals("base_x")).findFirst().orElseThrow();
        base.set(100.0);assertEquals(1.5,base.get());
        catalog.totemAnimation.applyPreset(catalog.totemAnimation.presets().get(1));assertEquals("Compact",catalog.totemAnimation.currentPresetName());
        var size=(DoubleSetting)catalog.totemAnimation.settings().stream().filter(s->s.id().equals("size")).findFirst().orElseThrow();
        size.set(-100.0);assertEquals(.15,size.get());assertEquals("Custom",catalog.totemAnimation.currentPresetName());
    }
    @Test void autoAlignmentKeepsEditedLegacyProbesButDefaultsUseTheActualModel() {
        ClientModules catalog=new ClientModules();assertTrue(catalog.weaponTrails.modelAlignment());
        var base=(DoubleSetting)catalog.weaponTrails.settings().stream().filter(s->s.id().equals("base_x")).findFirst().orElseThrow();
        base.set(.25);assertFalse(catalog.weaponTrails.modelAlignment());base.reset();assertTrue(catalog.weaponTrails.modelAlignment());
        catalog.weaponTrails.applyPreset(catalog.weaponTrails.presets().getFirst());assertTrue(catalog.weaponTrails.modelAlignment());
    }
}
