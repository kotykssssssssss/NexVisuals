package dev.nexvisuals.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @Shadow private ItemStack itemActivationItem;
    @Shadow private int itemActivationTicks;
    // The native model has a private push/pop; this hook neither changes the event nor its timer.
    @Inject(method = "renderItemActivationAnimation", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor(Lcom/mojang/blaze3d/platform/Lighting$Entry;)V"))
    private void nexvisuals$totem(PoseStack pose,float partialTick,SubmitNodeCollector collector,CallbackInfo ci) {
        if(NexVisualsClient.instance()!=null) NexVisualsClient.instance().totemAnimation().apply(pose,itemActivationItem,itemActivationTicks,partialTick);
    }
    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void nexvisuals$visibility(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null && NexVisualsClient.instance().fire().hidden()) ci.cancel();
    }
    // Each half has its own push/pop; applying inside that scope cannot leak to other overlays.
    @Inject(method = "renderFire", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private static void nexvisuals$transform(PoseStack pose, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null) NexVisualsClient.instance().fire().transform(pose);
    }
    @ModifyArg(method = "renderFire", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index = 3)
    private static float nexvisuals$alpha(float alpha) { return NexVisualsClient.instance() == null ? alpha : NexVisualsClient.instance().fire().alpha(alpha); }
}
