package dev.nexvisuals.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Passive probe after the pack's item transform; active only inside the local held-weapon scope. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
abstract class ItemModelTrailMixin {
    @Inject(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack$Pose;)V",shift=At.Shift.AFTER))
    private void nexvisuals$blade(PoseStack pose,SubmitNodeCollector collector,int light,int overlay,int outline,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if(mod!=null && mod.weaponTrails().enabled()) mod.weaponTrails().sample(pose);
    }
}
