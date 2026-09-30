package dev.nexvisuals.client.effect;

import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Plays only after the real local totem status event. No inference about other players. */
public final class TotemSoundsModule extends VisualModule {
    public enum Voice {
        VANILLA("minecraft:item.totem.use"), CHIME("minecraft:block.amethyst_block.chime"),
        BELL("minecraft:block.note_block.bell"), ARCADE("minecraft:block.note_block.bit"), SOFT("minecraft:block.note_block.harp"),
        CRICKET("nexvisuals:user_hit_cricket"), HIT_2("nexvisuals:user_hit_2"), CRITICAL("nexvisuals:user_hit_critical"),
        HITMARKER("nexvisuals:user_hitmarker"), ALPHA_DAMAGE("nexvisuals:user_alpha_damage"), OSU("nexvisuals:user_osu");
        private final SoundEvent sound;
        Voice(String id) {sound=SoundEvent.createVariableRangeEvent(Identifier.parse(id));}
        public SoundEvent sound() {return sound;}
    }
    private final EnumSetting<Voice> voice=add(new EnumSetting<>("preset","Sound","Local totem cue: vanilla, musical voices, or supplied hit clips.",Voice.CHIME,Voice.class));
    private final DoubleSetting volume=add(new DoubleSetting("volume","Volume","Custom cue respects master volume; native Vanilla also respects Players volume.",.6,0,1));
    private final DoubleSetting pitch=add(new DoubleSetting("pitch","Pitch","Custom cue playback pitch.",1,.5,2));
    private final DoubleSetting variation=add(new DoubleSetting("variation","Pitch variation","Random deviation for custom cues.",.04,0,.4));
    private final DoubleSetting vanillaVolume=add(new DoubleSetting("vanilla_volume","Vanilla underlay volume","Keep a quieter native cue under custom voices. Ignored for Vanilla voice. 1 preserves it; 0 replaces its audio only.",.2,0,1));
    private long last;
    private SimpleSoundInstance playing;
    public TotemSoundsModule() {
        super("totem_sounds","Totem Pop Sounds","Selectable local audio for your real totem activation; particles, item animation and gameplay remain vanilla.",Category.COMBAT);
        preset("Crystal Pop","Amethyst chime over a quiet native cue.");
        preset("Bell Pop","Bright bell.","preset","BELL","pitch",.9,"variation",0.0);
        preset("Arcade Pop","Short retro bit tone.","preset","ARCADE","pitch",.8);
        preset("Soft Pop","Gentle harp.","preset","SOFT","volume",.4);
        preset("Impact Pop","Use the supplied second hit clip.","preset","HIT_2","pitch",1.0,"variation",0.0);
        preset("Classic Pop","Use the supplied alpha damage clip.","preset","ALPHA_DAMAGE","pitch",1.0,"variation",0.0);
        preset("Vanilla","Native totem sound without a second copy.","preset","VANILLA","volume",1.0);
        action("Preview sound","Preview the cue without activating/counting a totem.",()->play(Minecraft.getInstance(),true));
        group("Sound",voice,volume,pitch,variation);group("Vanilla mix",vanillaVolume);
    }
    public float vanillaMultiplier() {return !enabled()?1:voice.get()==Voice.VANILLA?volume.get().floatValue():volume.get()==0?1:vanillaVolume.get().floatValue();}
    public float vanillaPitch(Minecraft client) {return !enabled() || voice.get()!=Voice.VANILLA?1:randomPitch(client);}
    public void popped(Minecraft client) {if(enabled() && voice.get()!=Voice.VANILLA) play(client,false);}
    private void play(Minecraft client,boolean preview) {
        if(client==null || !preview && (client.player==null || client.level==null) || System.nanoTime()-last<100_000_000L || volume.get()==0) return;
        last=System.nanoTime();
        if(!preview && voice.get()==Voice.VANILLA) return;
        if(playing!=null) client.getSoundManager().stop(playing);
        playing=SimpleSoundInstance.forUI(voice.get().sound(),randomPitch(client),volume.get().floatValue());
        client.getSoundManager().play(playing);
    }
    private float randomPitch(Minecraft client) {
        double random=client.level==null?Math.random():client.level.random.nextDouble();
        return (float)Math.clamp(pitch.get()+(random*2-1)*variation.get(),.5,2);
    }
    @Override protected void onDisable() {
        last=0;
        if(playing!=null) {var client=Minecraft.getInstance();if(client!=null) client.getSoundManager().stop(playing);playing=null;}
    }
}
