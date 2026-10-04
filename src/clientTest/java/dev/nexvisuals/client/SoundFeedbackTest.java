package dev.nexvisuals.client;

import com.google.gson.*;
import dev.nexvisuals.client.effect.HitSoundsModule;
import dev.nexvisuals.client.effect.TotemSoundsModule;
import dev.nexvisuals.client.hud.EditableHud;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class SoundFeedbackTest {
    @TempDir Path directory;
    private Setting<?> setting(VisualModule module,String id) {return module.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    @Test void guiAudioStatusMatchesTheActualBundledResourceMode() {
        var catalog = new ClientModules();
        boolean clips = getClass().getResource("/nexvisuals/user-audio.json") != null;
        assertTrue(catalog.sounds.runtimeStatus().contains(clips ? "Local audio build" : "vanilla fallback"));
        assertEquals(catalog.sounds.runtimeStatus(), catalog.totemSounds.runtimeStatus());
    }
    @Test void suppliedVoicesHaveResolvablePackagedDefinitionsWithFilesOrSafeVanillaFallbacks() throws Exception {
        JsonObject sounds;
        try(var input=getClass().getResourceAsStream("/assets/nexvisuals/sounds.json")) {
            assertNotNull(input);sounds=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var ids=new HashSet<String>();
        for(var voice:HitSoundsModule.Voice.values()) if(voice.userClip()) {
            var id=voice.sound().location();assertTrue(ids.add(id.getPath()));assertTrue(sounds.has(id.getPath()));
            var resource=sounds.getAsJsonObject(id.getPath()).getAsJsonArray("sounds").get(0).getAsJsonObject();
            String name=resource.get("name").getAsString();
            if(resource.get("type").getAsString().equals("file")) {
                assertTrue(name.startsWith("nexvisuals:user/"));
                try(var input=getClass().getResourceAsStream("/assets/nexvisuals/sounds/"+name.substring("nexvisuals:".length())+".ogg")) {
                    assertNotNull(input);byte[] bytes=input.readAllBytes();
                    assertEquals("OggS",new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII));
                    assertTrue(new String(bytes,0,Math.min(100,bytes.length),java.nio.charset.StandardCharsets.ISO_8859_1).contains("vorbis"));
                }
            } else {assertEquals("event",resource.get("type").getAsString());assertTrue(name.startsWith("minecraft:"));}
        }
        assertEquals(6,ids.size());assertEquals(6,sounds.size());
        for(var voice:TotemSoundsModule.Voice.values()) if(voice.sound().location().getNamespace().equals("nexvisuals")) assertTrue(ids.contains(voice.sound().location().getPath()));
    }
    @Test void privateBuildManifestHashesMatchTheActualSixOggsOrOrdinaryBuildContainsNoClips() throws Exception {
        try(var input=getClass().getResourceAsStream("/nexvisuals/user-audio.json")) {
            if(input==null) {
                assertNull(getClass().getResource("/assets/nexvisuals/sounds/user/hit_cricket.ogg"));return;
            }
            var clips=JsonParser.parseString(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("clips");
            assertEquals(6,clips.size());
            for(var entry:clips.entrySet()) {
                var metadata=entry.getValue().getAsJsonObject();assertEquals(1,metadata.get("channels").getAsInt());assertEquals(44100,metadata.get("sampleRate").getAsInt());
                double duration=metadata.get("durationSeconds").getAsDouble();assertTrue(duration>0&&duration<=5.01);
                try(var ogg=getClass().getResourceAsStream("/assets/nexvisuals/sounds/user/"+entry.getKey()+".ogg")) {
                    assertNotNull(ogg);assertEquals(metadata.get("oggSha256").getAsString(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ogg.readAllBytes())));
                }
            }
        }
    }
    @Test void audioPresetsAndTrackerPlacementPersistThroughConfigAndNamedProfiles() throws Exception {
        var c=new ClientModules();var globals=new GlobalSettings();
        c.sounds.applyPreset(c.sounds.presets().stream().filter(p->p.name().equals("Hitmarker")).findFirst().orElseThrow());
        ((DoubleSetting)setting(c.sounds,"vanilla_volume")).set(.15);c.sounds.setEnabled(true);
        c.totemSounds.applyPreset(c.totemSounds.presets().stream().filter(p->p.name().equals("Classic Pop")).findFirst().orElseThrow());c.totemSounds.setEnabled(true);
        assertInstanceOf(EditableHud.class,c.totemTracker);c.totemTracker.setEnabled(true);
        ((DoubleSetting)setting(c.totemTracker,"relative_x")).set(.7);((BooleanSetting)setting(c.totemTracker,"relative_position")).set(true);
        var config=new ConfigManager(directory.resolve("active.json"),c.registry,globals);config.save();var expected=config.snapshot();
        var fresh=new ClientModules();var restored=new ConfigManager(config.path(),fresh.registry,new GlobalSettings());
        assertTrue(restored.load().warnings().isEmpty());assertEquals(expected,restored.snapshot());
        var profiles=new ProfileManager(directory.resolve("profiles"),c.registry,globals);profiles.save("My Sounds");profiles.restoreDefaults();
        assertTrue(profiles.load("My Sounds").warnings().isEmpty());assertEquals(expected,config.snapshot());
    }
    @Test void nativeTotemVoiceDoesNotDoublePlayAndMutedCustomCueKeepsVanilla() {
        var c=new ClientModules();assertEquals(1,c.totemSounds.vanillaMultiplier());
        c.totemSounds.setEnabled(true);assertEquals(.2,c.totemSounds.vanillaMultiplier(),1e-6);
        ((DoubleSetting)setting(c.totemSounds,"volume")).set(0.0);assertEquals(1,c.totemSounds.vanillaMultiplier());
        c.totemSounds.applyPreset(c.totemSounds.presets().stream().filter(p->p.name().equals("Vanilla")).findFirst().orElseThrow());
        ((DoubleSetting)setting(c.totemSounds,"volume")).set(.7);assertEquals(.7,c.totemSounds.vanillaMultiplier(),1e-6);
        c.totemSounds.setEnabled(false);assertEquals(1,c.totemSounds.vanillaMultiplier());
    }
    @Test void oldSoundConfigKeepsItsVoiceAndMalformedNewSettingsAreIsolatedAndClamped() {
        var c=new ClientModules();var config=new ConfigManager(directory.resolve("old.json"),c.registry,new GlobalSettings());
        assertTrue(config.apply(JsonParser.parseString("""
            {"schemaVersion":1,"modules":{"hit_sounds":{"enabled":true,"settings":{"preset":"METALLIC","volume":0.32,"pitch":1.1,"variation":0.07}}}}
            """).getAsJsonObject()).isEmpty());
        assertEquals("METALLIC",setting(c.sounds,"preset").get().toString());assertFalse(c.totemSounds.enabled());assertFalse(c.totemTracker.enabled());
        var warnings=config.apply(JsonParser.parseString("""
            {"schemaVersion":1,"modules":{
             "hit_sounds":{"settings":{"preset":"MISSING","vanilla_volume":-100}},
             "totem_sounds":{"settings":{"pitch":99,"vanilla_volume":999}},
             "hud_totem_tracker":{"settings":{"scale":99,"future":true}}}}
            """).getAsJsonObject());
        assertEquals(1,warnings.size());assertEquals(0.0,setting(c.sounds,"vanilla_volume").get());
        assertEquals(2.0,setting(c.totemSounds,"pitch").get());assertEquals(1.0,setting(c.totemSounds,"vanilla_volume").get());
        assertEquals(3.0,setting(c.totemTracker,"scale").get());
    }
}
