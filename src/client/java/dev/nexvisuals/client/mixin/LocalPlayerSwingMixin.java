package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes the accepted animation only; vanilla swing logic and its packet remain untouched. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerSwingMixin {
    @Inject(method = "swing", at = @At("RETURN"))
    private void nexvisuals$started(InteractionHand hand, CallbackInfo ci) {
        var mod = NexVisualsClient.instance();
        if (mod == null || !mod.swing().enabled() && !mod.weaponTrails().enabled()) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        // LivingEntity sets -1 only for an accepted start/restart. Mining also makes rejected attempts.
        if (player.swinging && player.swingTime == -1 && player.swingingArm == hand) {
            mod.swing().started(player, hand);
            mod.weaponTrails().started(player, hand);
        }
    }
}
