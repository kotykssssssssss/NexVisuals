package dev.nexvisuals.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    // This site is after vanilla world/entity post effects and before GUI extraction, including fabulous mode.
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void nexvisuals$post(DeltaTracker delta,boolean renderWorld,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if(mod!=null && renderWorld && Minecraft.getInstance().isGameLoadFinished()) mod.post().render(delta);
    }
    @Inject(method="close",at=@At("HEAD"))
    private void nexvisuals$releasePost(CallbackInfo ci) { var mod=NexVisualsClient.instance(); if(mod!=null) mod.post().closeRenderer(); }
    @ModifyExpressionValue(method = "bobView", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarState;getInterpolatedBob(F)F"))
    private float nexvisuals$bob(float original) {
        return NexVisualsClient.instance() == null ? original : original * NexVisualsClient.instance().camera().bobScale();
    }
    @ModifyExpressionValue(method = "bobHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D"))
    private double nexvisuals$hurt(double original) {
        return NexVisualsClient.instance() == null ? original : original * NexVisualsClient.instance().camera().hurtScale();
    }
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void nexvisuals$fov(Camera camera, float partialTick, boolean worldFov, CallbackInfoReturnable<Float> cir) {
        Minecraft client = Minecraft.getInstance();
        if (worldFov && client.player != null && !client.player.isScoping() && !client.player.isDeadOrDying()
                && NexVisualsClient.instance() != null) cir.setReturnValue(NexVisualsClient.instance().camera().fov(cir.getReturnValue()));
    }
}
