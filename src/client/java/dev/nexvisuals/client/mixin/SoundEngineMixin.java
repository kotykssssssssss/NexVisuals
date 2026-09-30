package dev.nexvisuals.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Only scale matching attack instances. Identity, channel/category, subtitles and all other sounds stay native. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @ModifyExpressionValue(method={"play","calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F"},
            at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"))
    private float nexvisuals$attackMix(float original,SoundInstance sound) {
        var mod=NexVisualsClient.instance();
        return mod==null?original:mod.hitSounds().adjustVanilla(Minecraft.getInstance(),sound,original);
    }
}
