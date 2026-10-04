package dev.nexvisuals.client.effect;

import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import dev.nexvisuals.core.audio.LocalSoundWindow;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

/** Local attack feedback, including optional user-supplied clips converted at build time. */
public final class HitSoundsModule extends VisualModule {
    private static final String AUDIO_STATUS = HitSoundsModule.class.getResource("/nexvisuals/user-audio.json") != null
            ? "Local audio build: six supplied clips are bundled."
            : "Public audio build: supplied-clip slots use vanilla fallback sounds.";
    public static String audioStatus() { return AUDIO_STATUS; }
    @Override public String runtimeStatus() { return AUDIO_STATUS; }

    public enum Voice {
        SOFT("block.note_block.harp"), CLICK("ui.button.click"), POP("entity.chicken.egg"),
        BELL("block.note_block.bell"), METALLIC("block.chain.hit"), ARCADE("block.note_block.bit"),
        CRICKET("nexvisuals:user_hit_cricket"), HIT_2("nexvisuals:user_hit_2"), CRITICAL("nexvisuals:user_hit_critical"),
        HITMARKER("nexvisuals:user_hitmarker"), ALPHA_DAMAGE("nexvisuals:user_alpha_damage"), OSU("nexvisuals:user_osu");
        final SoundEvent sound;
        Voice(String id) { sound = SoundEvent.createVariableRangeEvent(Identifier.parse(id.contains(":")?id:"minecraft:"+id)); }
        public SoundEvent sound() {return sound;}
        public boolean userClip() {return sound.location().getNamespace().equals("nexvisuals");}
    }
    private final EnumSetting<Voice> voice = add(new EnumSetting<>("preset", "Sound", "Vanilla voices plus six optional supplied clips. Local user build includes the clips; ordinary builds retain safe fallback events.", Voice.SOFT, Voice.class));
    private final DoubleSetting volume = add(new DoubleSetting("volume", "Volume", "Local feedback volume; also respects Minecraft master volume.", .45, 0, 1));
    private final DoubleSetting pitch = add(new DoubleSetting("pitch", "Pitch", "Playback pitch.", 1.2, .5, 2));
    private final DoubleSetting variation = add(new DoubleSetting("variation", "Pitch variation", "Random pitch deviation on each local attack.", .08, 0, .4));
    private final BooleanSetting quieter = add(new BooleanSetting("quieter_vanilla", "Quieter vanilla attacks", "Reduce only attack sounds near your recent local attack; hurt/death/other sound categories stay unchanged.", true));
    private final DoubleSetting vanillaVolume = add(new DoubleSetting("vanilla_volume", "Vanilla attack volume", "Multiplier during a short local attack window. 0.25 is one quarter; 1 preserves vanilla.", .25, 0, 1));
    private final LocalSoundWindow window = new LocalSoundWindow();
    private long last;
    private SimpleSoundInstance userPlayback;
    public HitSoundsModule() {
        super("hit_sounds", "Hit Sounds", "Local attack-attempt feedback. Does not claim server-confirmed damage.", Category.COMBAT);
        action("Preview sound", "Play the selected sound locally.", () -> play(Minecraft.getInstance()));
        preset("Soft","Original quiet harp feedback.");
        preset("Click","Short vanilla button click.","preset","CLICK","pitch",1.0,"variation",0.0);
        preset("Bell","Bright bell feedback.","preset","BELL","volume",.3,"pitch",1.0);
        for(Voice clip:Voice.values()) if(clip.userClip())
            preset(switch(clip) {case CRICKET->"Cricket Bat";case HIT_2->"Hit 2";case CRITICAL->"Critical";case HITMARKER->"Hitmarker";case ALPHA_DAMAGE->"Alpha Damage";default->"Osu";},
                    "User clip at its original pitch, with quieter vanilla attacks.","preset",clip.name(),"pitch",1.0,"variation",0.0,"volume",.4);
        group("Sound",voice,volume,pitch,variation);group("Vanilla mix",quieter,vanillaVolume);
    }
    public void attacked(Minecraft client, Vec3 ignored) {
        if(!enabled() || client.player==null || client.level==null || volume.get()==0) return;
        window.arm(System.nanoTime(),client.level,client.player.getX(),client.player.getY(),client.player.getZ());
        play(client);
    }
    public float adjustVanilla(Minecraft client,SoundInstance sound,float original) {
        if(!enabled() || !quieter.get() || volume.get()==0 || vanillaVolume.get()==1 || sound.getSource()!=SoundSource.PLAYERS || sound.isRelative()) return original;
        var id=sound.getIdentifier();
        if(!LocalSoundWindow.isVanillaAttack(id.getNamespace(),id.getPath())) return original;
        return window.contains(System.nanoTime(),client.level,sound.getX(),sound.getY(),sound.getZ())?original*vanillaVolume.get().floatValue():original;
    }
    public void tick(Minecraft client) {window.expire(System.nanoTime(),client.level);}
    @Override protected void onDisable() {
        window.clear();last=0;
        if(userPlayback!=null) {var client=Minecraft.getInstance();if(client!=null) client.getSoundManager().stop(userPlayback);userPlayback=null;}
    }
    private void play(Minecraft client) {
        if (client==null || System.nanoTime() - last < 60_000_000L) return;
        last = System.nanoTime();
        double random=client.level==null?Math.random():client.level.random.nextDouble();
        float p = (float) Math.clamp(pitch.get() + (random * 2 - 1) * variation.get(), .5, 2);
        var sound=SimpleSoundInstance.forUI(voice.get().sound, p, volume.get().floatValue());
        if(userPlayback!=null) {client.getSoundManager().stop(userPlayback);userPlayback=null;}
        if(voice.get().userClip()) userPlayback=sound;
        client.getSoundManager().play(sound);
    }
}
