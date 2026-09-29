package dev.nexvisuals.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.client.gui.title.ConsoleTitleState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Decorates around the original render; vanilla buttons, Realms notices and credits still render. */
@Mixin(TitleScreen.class)
abstract class TitleScreenMixin {
    @Unique private ConsoleTitleState nexvisuals$state() {
        var mod=NexVisualsClient.instance();
        return mod!=null && mod.consoleMenu().enabled() ? mod.consoleMenu().titleState((TitleScreen)(Object)this) : null;
    }
    @Inject(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void nexvisuals$panel(GuiGraphics g,int x,int y,float delta,CallbackInfo ci) {
        var state=nexvisuals$state(); if(state!=null) state.beforeWidgets(g);
    }
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private void nexvisuals$logo(LogoRenderer logo,GuiGraphics g,int width,float alpha,Operation<Void> original) {
        var state=nexvisuals$state();
        if(state==null) { original.call(logo,g,width,alpha); return; }
        g.pose().pushMatrix();
        try { g.pose().translate(state.logoOffsetX(),state.logoOffsetY()); original.call(logo,g,width,alpha); }
        finally { g.pose().popMatrix(); }
    }
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;F)V"))
    private void nexvisuals$splash(SplashRenderer splash,GuiGraphics g,int width,Font font,float alpha,Operation<Void> original) {
        var state=nexvisuals$state();
        if(state==null) { original.call(splash,g,width,font,alpha); return; }
        if(NexVisualsClient.instance().consoleMenu().reduceMotion.get()) return;
        g.pose().pushMatrix();
        try { g.pose().translate(state.logoOffsetX(),state.logoOffsetY()); original.call(splash,g,width,font,alpha); }
        finally { g.pose().popMatrix(); }
    }
}
