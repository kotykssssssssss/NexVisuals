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
    @Inject(method="renderBackground",at=@At("HEAD"),cancellable=true)
    private void nexvisuals$pauseWallpaper(GuiGraphics graphics,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if((Object)this instanceof net.minecraft.client.gui.screens.PauseScreen && mod!=null && mod.liveBackground().render((Screen)(Object)this,graphics)) {
            Minecraft.getInstance().gui.renderDeferredSubtitles();
            ci.cancel();
        }
    }
    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void nexvisuals$backdrop(GuiGraphics graphics, CallbackInfo ci) {
        NexVisualsClient mod = NexVisualsClient.instance();
        if (mod != null && mod.backdrop().enabled() && Minecraft.getInstance().level != null) {
            mod.backdrop().render((Screen) (Object) this, graphics);
            ci.cancel();
        }
    }
}
