package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.core.visual.SkyMath;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Only ordinary atmospheric fog; density can increase, never remove or extend vanilla fog. */
@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
    @ModifyArgs(method="setupFog",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void nexvisuals$fog(Args args,Camera camera,int distance,DeltaTracker delta,float darkness,ClientLevel level) {
        var mod=NexVisualsClient.instance();
        if(mod==null || !mod.skybox().fog.get() || !mod.skybox().eligible(level,camera)) return;
        var module=mod.skybox();
        int tint=module.currentFogColor(camera,delta.getGameTimeDeltaPartialTick(false));
        float blend=module.fogBlend.get().floatValue()*(tint>>>24)/255f;
        // Mutate the computed color too: setupFog returns this object for the matching clear color.
        Vector4f color=args.get(2);
        color.x+=( ((tint>>>16)&255)/255f-color.x)*blend;
        color.y+=( ((tint>>>8)&255)/255f-color.y)*blend;
        color.z+=( (tint&255)/255f-color.z)*blend;
        for(int index=3;index<=8;index++) args.set(index,SkyMath.fogDistance(args.<Float>get(index),module.fogDensity.get()));
    }
}
