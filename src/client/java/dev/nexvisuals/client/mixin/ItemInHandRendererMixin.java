package dev.nexvisuals.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
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
            NexVisualsClient.instance().viewmodel().apply(pose, hand, player.getMainArm());
            NexVisualsClient.instance().shield().apply(pose, player, hand, stack);
        }
    }
    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void nexvisuals$swing(float progress, PoseStack pose, int side, HumanoidArm arm, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null && NexVisualsClient.instance().swing().apply(pose, arm)) ci.cancel();
    }
    // Establish a narrow first-person scope. The model layer hook samples after its actual display transform.
    @WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"), require = 2)
    private void nexvisuals$trail(ItemInHandRenderer renderer, LivingEntity player, ItemStack stack,
                                 ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector,
                                 int light, Operation<Void> original) {
        var mod=NexVisualsClient.instance();
        if(mod==null || !mod.weaponTrails().enabled()) { original.call(renderer,player,stack,context,pose,collector,light);return; }
        mod.weaponTrails().begin(player,context,stack,pose,mod.swing());
        try { original.call(renderer,player,stack,context,pose,collector,light); }
        finally { mod.weaponTrails().end(collector); }
    }
}
