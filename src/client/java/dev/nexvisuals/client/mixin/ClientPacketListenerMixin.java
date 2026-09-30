package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla has already processed its event on the client thread. No packet mutation or cancellation. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method="handleEntityEvent",at=@At("RETURN"))
    private void nexvisuals$localTotem(ClientboundEntityEventPacket packet,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if(mod==null || !mod.totemEcho().enabled() || packet.getEventId()!=35) return;
        var client=Minecraft.getInstance();
        if(client.player!=null && client.level!=null && packet.getEntity(client.level)==client.player) mod.totemEcho().popped(client);
    }
}
