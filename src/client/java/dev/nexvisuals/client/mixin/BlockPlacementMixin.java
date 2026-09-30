package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the normal placement cell before/after vanilla use; no guesses about remote actions. */
@Mixin(MultiPlayerGameMode.class)
public abstract class BlockPlacementMixin {
    @Inject(method="useItemOn",at=@At("HEAD"))
    private void nexvisuals$beforePlace(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir) {
        var mod=NexVisualsClient.instance();
        if(mod!=null && mod.blockEffects().enabled()) mod.blockEffects().beforePlace(Minecraft.getInstance(),player,hand,hit);
    }
    @Inject(method="useItemOn",at=@At("RETURN"))
    private void nexvisuals$afterPlace(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir) {
        var mod=NexVisualsClient.instance();
        if(mod!=null && mod.blockEffects().enabled()) mod.blockEffects().afterPlace(Minecraft.getInstance(),cir.getReturnValue());
    }
}
