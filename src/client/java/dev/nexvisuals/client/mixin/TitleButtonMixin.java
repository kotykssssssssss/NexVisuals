package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the title state's original button instances get a skin; interaction is untouched. */
@Mixin(Button.Plain.class)
abstract class TitleButtonMixin {
    @Inject(method="renderContents",at=@At("HEAD"),cancellable=true)
    private void nexvisuals$skin(GuiGraphics g,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        var mod=NexVisualsClient.instance();
        if(mod==null || !mod.consoleMenu().enabled()) return;
        var state=mod.consoleMenu().titleState(Minecraft.getInstance().screen);
        if(state!=null && state.skin((Button)(Object)this,g)) ci.cancel();
    }
}
