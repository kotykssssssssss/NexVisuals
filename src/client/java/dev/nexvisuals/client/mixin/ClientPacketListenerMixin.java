package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla has already processed its event on the client thread. No packet mutation or cancellation. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    // After vanilla's thread handoff, before vanilla reduces/removes the picked-up ItemEntity.
    @Inject(method="handleTakeItemEntity", at=@At(value="INVOKE", target=
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift=At.Shift.AFTER))
    private void nexvisuals$localPickup(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        var mod = NexVisualsClient.instance();
        if (mod == null || !mod.pickups().enabled()) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || packet.getPlayerId() != client.player.getId()) return;
        if (client.level.getEntity(packet.getItemId()) instanceof ItemEntity item) {
            mod.pickups().pickedUp(client, item.getItem(), packet.getAmount());
        }
    }

    @Inject(method="handleEntityEvent",at=@At("RETURN"))
    private void nexvisuals$localTotem(ClientboundEntityEventPacket packet,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if(mod==null || packet.getEventId()!=35) return;
        if(!mod.totemEcho().enabled() && !mod.totemSounds().enabled() && !mod.totemTracker().enabled()) return;
        var client=Minecraft.getInstance();
        if(client.player!=null && client.level!=null && packet.getEntity(client.level)==client.player) {
            mod.totemEcho().popped(client);mod.totemSounds().popped(client);mod.totemTracker().popped(client);
        }
    }
    @ModifyArgs(method="handleEntityEvent",at=@At(value="INVOKE",
            target="Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void nexvisuals$totemMix(Args args,ClientboundEntityEventPacket packet) {
        var mod=NexVisualsClient.instance();
        if(mod==null || !mod.totemSounds().enabled() || packet.getEventId()!=35) return;
        var client=Minecraft.getInstance();
        if(client.player!=null && client.level!=null && packet.getEntity(client.level)==client.player) {
            args.set(5,((Float)args.get(5))*mod.totemSounds().vanillaMultiplier());
            args.set(6,((Float)args.get(6))*mod.totemSounds().vanillaPitch(client));
        }
    }
}
