package dev.nexvisuals.client;

import com.google.gson.*;
import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.Setting;
import dev.nexvisuals.core.visual.ParticleBudget;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ParticlePolishTest {
    @TempDir Path directory;
    private Setting<?> setting(VisualModule module,String id) { return module.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(); }
    @Test void everyOldParticlePresetRetainsEveryPreviouslyEffectiveValue() throws Exception {
        var catalog=new ClientModules();
        try(var input=getClass().getResourceAsStream("/nexvisuals/legacy-particle-presets.json")) {
            assertNotNull(input);
            var baseline=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            int checked=0;
            for(var moduleEntry:baseline.entrySet()) {
                var module=catalog.registry.find(moduleEntry.getKey()).orElseThrow();
                for(var recipe:moduleEntry.getValue().getAsJsonObject().entrySet()) {
                    var preset=module.presets().stream().filter(p->p.name().equals(recipe.getKey())).findFirst().orElseThrow();
                    module.applyPreset(preset);
                    assertEquals(recipe.getKey(),module.currentPresetName(),module.id());
                    for(var value:recipe.getValue().getAsJsonObject().entrySet()) assertEquals(value.getValue(),setting(module,value.getKey()).toJson(),module.id()+" / "+recipe.getKey()+" / "+value.getKey());
                    for(var value:module.settings()) if(value.id().startsWith("particle_")) assertEquals(value.defaultValue(),value.get(),module.id()+" / neutral "+value.id());
                    checked++;
                }
            }
            assertTrue(checked>=45);
        }
    }
    @Test void newStylesAndCommonSettingsSurviveRestartProfilesAndReset() throws Exception {
        var catalog=new ClientModules();var globals=new GlobalSettings();
        var config=new ConfigManager(directory.resolve("active.json"),catalog.registry,globals);
        for(var module:catalog.registry.all()) {
            if(module.presets().isEmpty()) continue;
            module.applyPreset(module.presets().getLast());
        }
        globals.particleQuality.set(ParticleBudget.Quality.LOW);
        setting(catalog.hits,"particle_color_mode").fromJson(new JsonPrimitive("RAINBOW"));
        setting(catalog.hits,"particle_size_variance").fromJson(new JsonPrimitive(.35));
        setting(catalog.fireflies,"particle_shape").fromJson(new JsonPrimitive("DIAMOND"));
        catalog.fireflies.setEnabled(true);config.save();var snapshot=config.snapshot();
        var next=new ClientModules();var nextGlobals=new GlobalSettings();
        var restored=new ConfigManager(config.path(),next.registry,nextGlobals);
        assertTrue(restored.load().warnings().isEmpty());assertEquals(snapshot,restored.snapshot());
        var profiles=new ProfileManager(directory.resolve("profiles"),catalog.registry,globals);
        profiles.save("Particle Look");profiles.restoreDefaults();
        assertFalse(catalog.fireflies.enabled());assertEquals(ParticleBudget.Quality.HIGH,globals.particleQuality.get());
        assertTrue(profiles.load("Particle Look").warnings().isEmpty());assertEquals(snapshot,config.snapshot());
    }
    @Test void olderPartialConfigsPreserveSavedSizesAndDefaultOnlyTheMissingControls() {
        var catalog=new ClientModules();var globals=new GlobalSettings();
        var config=new ConfigManager(directory.resolve("old.json"),catalog.registry,globals);
        var root=JsonParser.parseString("{\"schemaVersion\":1,\"modules\":{\"player_trails\":{\"enabled\":true,\"settings\":{\"size\":0.13}},\"hit_visuals\":{\"settings\":{\"size\":1.0}}}}").getAsJsonObject();
        assertTrue(config.apply(root).isEmpty());
        assertEquals(.13,setting(catalog.trails,"size").get());assertEquals(1.,setting(catalog.hits,"size").get());
        assertEquals(ParticleBudget.Quality.HIGH,globals.particleQuality.get());
        assertEquals(1.,setting(catalog.trails,"particle_opacity").get());
        assertFalse((Boolean)setting(catalog.trails,"particle_envelope").get());
        setting(catalog.hits,"particle_size_variance").fromJson(new JsonPrimitive(999));
        assertEquals(.5,setting(catalog.hits,"particle_size_variance").get());
        setting(catalog.hits,"particle_max_distance").fromJson(new JsonPrimitive(-1));
        assertEquals(8.,setting(catalog.hits,"particle_max_distance").get());
    }
    @Test void contextDependentControlsAndCachedSnapshotsFollowActualEdits() {
        var list=new java.util.ArrayList<Setting<?>>();var appearance=new ParticleAppearance(list::add,ParticleAppearance.Kind.FREE);
        assertFalse(appearance.rainbowSpeed.visible());assertFalse(appearance.fadeIn.visible());
        appearance.colorMode.fromJson(new JsonPrimitive("RAINBOW"));assertTrue(appearance.rainbowSpeed.visible());
        appearance.envelope.set(true);assertTrue(appearance.fadeIn.visible());
        var first=appearance.snapshot();assertSame(first,appearance.snapshot());
        appearance.drag.set(.8);var second=appearance.snapshot();assertNotSame(first,second);assertEquals(.8,second.drag());
        appearance.spritesWhen(()->false);assertFalse(appearance.shape.visible());assertFalse(appearance.glow.visible());
        var ground=new ParticleAppearance(s->{},ParticleAppearance.Kind.GROUND);assertFalse(ground.gravity.visible());
    }
    @Test void bothNewModulesAreDistinctDisabledAndTheirRecipesPersist() throws Exception {
        var catalog=new ClientModules();var globals=new GlobalSettings();
        var config=new ConfigManager(directory.resolve("new.json"),catalog.registry,globals);
        for(var module:java.util.List.of(catalog.consumption,catalog.rainRipples)) {
            assertFalse(module.enabled());var recipes=new java.util.HashSet<JsonObject>();
            for(var preset:module.presets()) {
                module.applyPreset(preset);assertEquals(preset.name(),module.currentPresetName());
                assertTrue(recipes.add(config.snapshot()));
            }
            module.setEnabled(true);
        }
        config.save();var next=new ClientModules();var restored=new ConfigManager(config.path(),next.registry,new GlobalSettings());
        assertTrue(restored.load().warnings().isEmpty());assertEquals(config.snapshot(),restored.snapshot());
        var old=new ConfigManager(directory.resolve("empty.json"),new ClientModules().registry,new GlobalSettings());
        assertTrue(old.apply(JsonParser.parseString("{\"schemaVersion\":1,\"modules\":{}}").getAsJsonObject()).isEmpty());
        assertDoesNotThrow(()->new ClientModules().consumption.tick(null));
        assertDoesNotThrow(()->new ClientModules().rainRipples.tick(null));
    }
    @Test void newCallbacksNeverQueryRemoteEntitiesPacketsOrModifyItemOrWeatherState() throws Exception {
        for(String name:java.util.List.of("ConsumptionEffectsModule","RainRipplesModule")) {
            try(var stream=getClass().getResourceAsStream("/dev/nexvisuals/client/cosmetic/"+name+".class")) {
                assertNotNull(stream);var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(stream).accept(node,0);
                for(var method:node.methods) for(var instruction:method.instructions) if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call) {
                    assertFalse(java.util.Set.of("send","sendPacket","getEntities","getEntitiesOfClass","entitiesForRendering","consume","finishUsingItem","shrink","setRainLevel","setDayTime").contains(call.name),name+" / "+call.name);
                }
            }
        }
    }
    @Test void explicitColorModesOverrideLegacyRainbowAndAlternatingComponentSources() {
        var appearance=new ParticleAppearance(s->{},ParticleAppearance.Kind.FREE);
        assertEquals(0xFFABCDEF,appearance.componentColor(0xFF123456,0xFFABCDEF));
        for(var mode:dev.nexvisuals.core.visual.ParticleTuning.ColorMode.values()) if(mode!=dev.nexvisuals.core.visual.ParticleTuning.ColorMode.LEGACY) {
            appearance.colorMode.set(mode);assertEquals(0xFF123456,appearance.componentColor(0xFF123456,0xFFABCDEF));
        }
    }
    @Test void oldProceduralDropsAndSiltRetainNeutralNewParameters() {
        var catalog=new ClientModules();
        for(var preset:catalog.weatherLens.presets().subList(0,3)) {
            catalog.weatherLens.applyPreset(preset);assertEquals(1.,catalog.weatherLens.dropSize.get());
            assertEquals(1.,catalog.weatherLens.randomness.get());assertEquals(0,catalog.weatherLens.dropTint.get()>>>24);
        }
        for(var preset:catalog.underwater.presets().subList(0,3)) {
            catalog.underwater.applyPreset(preset);assertEquals(1.,catalog.underwater.siltSize.get());
            assertEquals(1.,catalog.underwater.siltDensity.get());assertEquals(1.,catalog.underwater.siltSpeed.get());
            assertEquals(0xFFFFFFFF,catalog.underwater.siltColor.get());
            assertEquals(dev.nexvisuals.client.post.UnderwaterEffectsModule.SpeckShape.SOFT,catalog.underwater.siltShape.get());
        }
    }
}
