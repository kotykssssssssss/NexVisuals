package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Replace the entire panorama (including its overlay) only after a live draw succeeds; retain themed vanilla motion. */
@Mixin(PanoramaRenderer.class)
abstract class PanoramaRendererMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void nexvisuals$live(GuiGraphics graphics,int width,int height,boolean spin,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if(mod!=null && mod.liveBackground().render(Minecraft.getInstance().screen,graphics)) ci.cancel();
    }
    @ModifyArgs(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/CubeMap;render(Lnet/minecraft/client/Minecraft;FF)V"))
    private void nexvisuals$motion(Args args) {
        var mod=NexVisualsClient.instance();
        if(mod!=null && mod.consoleMenu().affectsPanorama(Minecraft.getInstance().screen)) {
            var angles=mod.consoleMenu().angles(); args.set(1,angles.pitch()); args.set(2,angles.yaw());
        }
    }
}
