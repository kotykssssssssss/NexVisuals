package dev.nexvisuals.client.effect;

import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;

/** References vanilla assets already installed by Minecraft; no redistributed third-party audio. */
public final class HitSoundsModule extends VisualModule {
    public enum Voice {
        SOFT("block.note_block.harp"), CLICK("ui.button.click"), POP("entity.chicken.egg"),
        BELL("block.note_block.bell"), METALLIC("block.chain.hit"), ARCADE("block.note_block.bit");
        final SoundEvent sound;
        Voice(String id) { sound = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(id)); }
    }
    private final EnumSetting<Voice> voice = add(new EnumSetting<>("preset", "Sound", "Uses Minecraft's installed sounds; resource packs can replace them.", Voice.SOFT, Voice.class));
    private final DoubleSetting volume = add(new DoubleSetting("volume", "Volume", "Local feedback volume; also respects Minecraft UI/master volume.", .45, 0, 1));
    private final DoubleSetting pitch = add(new DoubleSetting("pitch", "Pitch", "Playback pitch.", 1.2, .5, 2));
    private final DoubleSetting variation = add(new DoubleSetting("variation", "Pitch variation", "Random pitch deviation on each local attack.", .08, 0, .4));
    private long last;
    public HitSoundsModule() {
        super("hit_sounds", "Hit Sounds", "Local attack-attempt feedback. Does not claim server-confirmed damage.", Category.COMBAT);
        action("Preview sound", "Play the selected sound locally.", () -> play(Minecraft.getInstance()));
    }
    public void attacked(Minecraft client, Vec3 ignored) { if (enabled()) play(client); }
    private void play(Minecraft client) {
        if (client.level == null || System.nanoTime() - last < 60_000_000L) return;
        last = System.nanoTime();
        float p = (float) Math.clamp(pitch.get() + (client.level.random.nextDouble() * 2 - 1) * variation.get(), .5, 2);
        client.getSoundManager().play(SimpleSoundInstance.forUI(voice.get().sound, p, volume.get().floatValue()));
    }
}
