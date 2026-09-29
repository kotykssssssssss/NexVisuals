package dev.nexvisuals.client.effect;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Local visible attack attempts only. PASS preserves vanilla/server authority over damage. */
public final class LocalAttackFeedback {
    private LocalAttackFeedback() { }
    @FunctionalInterface public interface Listener { void attacked(Minecraft client, Vec3 position); }
    public static void register(Listener... listeners) {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            Minecraft client = Minecraft.getInstance();
            if (level.isClientSide() && player == client.player && !player.isSpectator()
                    && client.screen == null && client.hitResult instanceof EntityHitResult target
                    && target.getEntity() == entity && !entity.isInvisibleTo(player)) {
                for (Listener listener : listeners) listener.attacked(client, target.getLocation());
            }
            return InteractionResult.PASS;
        });
    }
}
