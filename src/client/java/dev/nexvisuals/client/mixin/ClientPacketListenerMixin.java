package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla has already processed its event on the client thread. No packet mutation or cancellation. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
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
