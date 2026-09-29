package dev.nexvisuals.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    // The first push belongs to this hand; vanilla pops it after submitting the hand/item.
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
    private void nexvisuals$transform(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                     float swing, ItemStack stack, float equip, PoseStack pose, SubmitNodeCollector collector,
                                     int light, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null) {
            NexVisualsClient.instance().viewmodel().apply(pose, hand);
            NexVisualsClient.instance().shield().apply(pose, player, hand, stack);
        }
    }
    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void nexvisuals$swing(float progress, PoseStack pose, int side, HumanoidArm arm, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null && NexVisualsClient.instance().swing().apply(pose, arm, progress)) ci.cancel();
    }
}
