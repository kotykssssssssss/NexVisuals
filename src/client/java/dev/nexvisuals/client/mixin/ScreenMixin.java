package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void nexvisuals$backdrop(GuiGraphics graphics, CallbackInfo ci) {
        NexVisualsClient mod = NexVisualsClient.instance();
        if (mod != null && mod.backdrop().enabled() && Minecraft.getInstance().level != null) {
            mod.backdrop().render((Screen) (Object) this, graphics);
            ci.cancel();
        }
    }
}
