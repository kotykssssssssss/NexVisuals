package dev.nexvisuals.client;

import com.google.gson.JsonParser;
import dev.nexvisuals.client.hud.EditableHud;
import dev.nexvisuals.client.particle.EffectParticle;
import dev.nexvisuals.core.color.ColorPickerModel;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HudCustomizationTest {
    @TempDir Path directory;
    private static final List<String> IDS=List.of("hud_equipment","hud_item_counter","hud_status_effects","hud_active_modules");
    private Setting<?> setting(VisualModule m,String id) {return m.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    @Test void fourNewHudElementsUseExistingEditorPlacementAndPresetContract() {
        var c=new ClientModules();
        for(var id:IDS) {
            var m=c.registry.find(id).orElseThrow();assertInstanceOf(EditableHud.class,m);assertFalse(m.enabled());
            for(String key:List.of("anchor","scale","relative_position","relative_x","relative_y","color","background_color")) assertNotNull(setting(m,key));
            var snapshots=new HashSet<com.google.gson.JsonObject>();
            var config=new ConfigManager(directory.resolve("active.json"),c.registry,new GlobalSettings());
            for(var preset:m.presets()) {m.applyPreset(preset);assertEquals(preset.name(),m.currentPresetName());assertTrue(snapshots.add(config.snapshot()));}
            m.settings().forEach(Setting::reset);assertEquals(setting(m,"anchor").defaultValue(),setting(m,"anchor").get());
        }
    }
    @Test void hudPlacementPickerAlphaAndAnimationsSurviveConfigAndProfiles() throws Exception {
        var c=new ClientModules();var globals=new GlobalSettings();
        for(var id:IDS) {
            var m=c.registry.find(id).orElseThrow();m.applyPreset(m.presets().get(1));m.setEnabled(true);
            ((BooleanSetting)setting(m,"relative_position")).set(true);
            ((DoubleSetting)setting(m,"relative_x")).set(.73);((DoubleSetting)setting(m,"relative_y")).set(.41);
            new ColorPickerModel((ColorSetting)setting(m,"color")).hsv(.58,.64,.82);
            new ColorPickerModel((ColorSetting)setting(m,"background_color")).alpha(93);
        }
        var crosshair=c.registry.find("custom_crosshair").orElseThrow();crosshair.applyPreset(crosshair.presets().stream().filter(p->p.name().equals("Breathing Orbit")).findFirst().orElseThrow());
        var tint=c.registry.find("screen_tint").orElseThrow();tint.applyPreset(tint.presets().get(1));
        c.hits.applyPreset(c.hits.presets().stream().filter(p->p.name().equals("Hearts")).findFirst().orElseThrow());
        var config=new ConfigManager(directory.resolve("active.json"),c.registry,globals);config.save();var expected=config.snapshot();
        var fresh=new ClientModules();var restored=new ConfigManager(config.path(),fresh.registry,new GlobalSettings());
        assertTrue(restored.load().warnings().isEmpty());assertEquals(expected,restored.snapshot());
        var profiles=new ProfileManager(directory.resolve("profiles"),c.registry,globals);profiles.save("HUD and Hearts");profiles.rename("HUD and Hearts","Local Style");
        profiles.restoreDefaults();assertTrue(profiles.load("Local Style").warnings().isEmpty());assertEquals(expected,config.snapshot());
    }
    @Test void oldCrosshairAndTintStayStaticAndNewHudDefaultsOff() {
        var c=new ClientModules();var config=new ConfigManager(directory.resolve("old.json"),c.registry,new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("""
            {"schemaVersion":1,"modules":{
              "custom_crosshair":{"enabled":true,"settings":{"shape":"CIRCLE","size":8,"color":"#AABBCCDD"}},
              "screen_tint":{"enabled":true,"settings":{"color":"#553399FF","size":42}},
              "skybox":{"enabled":true,"settings":{"night_sky":"#FF241344"}}}}
            """).getAsJsonObject()).isEmpty());
        assertEquals("OFF",setting(c.registry.find("custom_crosshair").orElseThrow(),"motion").get().toString());
        assertEquals("STATIC",setting(c.registry.find("screen_tint").orElseThrow(),"mode").get().toString());
        assertEquals(0xFF241344,c.skybox.nightSky.get());
        for(var id:IDS) assertFalse(c.registry.find(id).orElseThrow().enabled());
    }
    @Test void newEnumsAndColorValidationAreIsolatedAndNumericsAreBounded() {
        var c=new ClientModules();var config=new ConfigManager(directory.resolve("bad.json"),c.registry,new GlobalSettings());
        var warnings=config.apply(JsonParser.parseString("""
            {"schemaVersion":1,"modules":{
              "hud_equipment":{"settings":{"layout":"MISSING","warning_threshold":999}},
              "hud_status_effects":{"settings":{"max_rows":999}},
              "hud_active_modules":{"settings":{"max_rows":999,"relative_x":-9}},
              "screen_tint":{"settings":{"mode":"UNKNOWN","health_threshold":0}},
              "custom_crosshair":{"settings":{"motion_amount":999,"color":"broken"}}}}
            """).getAsJsonObject());
        assertEquals(3,warnings.size());
        assertEquals(.6,setting(c.registry.find("hud_equipment").orElseThrow(),"warning_threshold").get());
        assertEquals(12,setting(c.registry.find("hud_status_effects").orElseThrow(),"max_rows").get());
        assertEquals(24,setting(c.registry.find("hud_active_modules").orElseThrow(),"max_rows").get());
        assertEquals(.1,setting(c.registry.find("screen_tint").orElseThrow(),"health_threshold").get());
        assertEquals(1.0,setting(c.registry.find("custom_crosshair").orElseThrow(),"motion_amount").get());
    }
    @Test void everyParticleShapeHasAtlasEntryAndPackagedProceduralMask() throws Exception {
        // Minecraft and the mod each contribute to this atlas; classpath order is not resource-pack merging.
        var sprites=new HashSet<String>();
        var atlases=getClass().getClassLoader().getResources("assets/minecraft/atlases/particles.json");
        while(atlases.hasMoreElements()) try(var input=atlases.nextElement().openStream()) {
            var entries=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("sources");
            for(var entry:entries) if(entry.getAsJsonObject().has("sprite")) sprites.add(entry.getAsJsonObject().get("sprite").getAsString());
        }
            for(var shape:EffectParticle.Shape.values()) {
                String name=shape.name().toLowerCase(Locale.ROOT);assertTrue(sprites.contains("nexvisuals:"+name));
                try(var png=getClass().getResourceAsStream("/assets/nexvisuals/textures/particle/"+name+".png")) {
                    assertNotNull(png);var image=javax.imageio.ImageIO.read(png);assertNotNull(image);assertEquals(128,image.getWidth());
                    if(shape==EffectParticle.Shape.PIXEL) {assertEquals(255,image.getRGB(64,64)>>>24);assertEquals(0,image.getRGB(0,0)>>>24);}
                    if(shape==EffectParticle.Shape.HEART) {assertEquals(255,image.getRGB(64,64)>>>24);assertEquals(0,image.getRGB(0,0)>>>24);}
                }
            }
    }
}
