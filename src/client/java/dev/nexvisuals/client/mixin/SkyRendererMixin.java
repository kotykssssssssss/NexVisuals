package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void nexvisuals$palette(ClientLevel level, float delta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null) NexVisualsClient.instance().sky().apply(state);
    }
}
