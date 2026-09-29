package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Reuses Minecraft's cubemap and render pipeline; no additional GPU buffers or framebuffers. */
@Mixin(PanoramaRenderer.class)
abstract class PanoramaRendererMixin {
    @ModifyArgs(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/CubeMap;render(Lnet/minecraft/client/Minecraft;FF)V"))
    private void nexvisuals$motion(Args args) {
        var mod=NexVisualsClient.instance();
        if(mod!=null && mod.consoleMenu().affectsPanorama(Minecraft.getInstance().screen)) {
            var angles=mod.consoleMenu().angles(); args.set(1,angles.pitch()); args.set(2,angles.yaw());
        }
    }
}
