package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.client.gui.title.ConsoleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Background-only theming leaves the real chunk map, progress, narration and completion untouched. */
@Mixin(LevelLoadingScreen.class)
abstract class LevelLoadingScreenMixin {
    @Shadow private LevelLoadingScreen.Reason reason;
    @Inject(method="renderBackground",at=@At("HEAD"),cancellable=true)
    private void nexvisuals$background(GuiGraphics g,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if(mod==null || !mod.consoleMenu().enabled() || !mod.consoleMenu().loading.get() || reason!=LevelLoadingScreen.Reason.OTHER) return;
        var theme=mod.consoleMenu();
        Minecraft.getInstance().gameRenderer.getPanorama().render(g,g.guiWidth(),g.guiHeight(),!theme.reduceMotion.get());
        ConsoleTheme.backdrop(g,theme,true);
        ConsoleTheme.loadingFooter(g,theme);
        ci.cancel();
    }
}
