package dev.nexvisuals.client.mixin;

import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.client.interfacefx.ContainerVisualState;
import dev.nexvisuals.client.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes clicks and decorates frames; never cancels input or touches menu/packet state. */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerScreenMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow protected Slot hoveredSlot;
    @Shadow @Final protected AbstractContainerMenu menu;
    @Unique private final ContainerVisualState nexvisuals$visuals = new ContainerVisualState();
    @Inject(method = "init", at = @At("TAIL"))
    private void nexvisuals$opened(CallbackInfo ci) { nexvisuals$visuals.reset(); }
    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void nexvisuals$panel(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null) nexvisuals$visuals.panel(graphics, leftPos, topPos, imageWidth, imageHeight, NexVisualsClient.instance().containers());
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void nexvisuals$feedback(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null) nexvisuals$visuals.render(graphics, leftPos, topPos, hoveredSlot, NexVisualsClient.instance().containers());
    }
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void nexvisuals$slot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (NexVisualsClient.instance() == null) return;
        var module = NexVisualsClient.instance().containers();
        if (!module.enabled()) return;
        Draw.rect(graphics, slot.x, slot.y, 16, 16, module.slotTint.get());
        if (module.slotOutline.get()) Draw.border(graphics, slot.x-1, slot.y-1, 18, 18, 1, Draw.withAlpha(module.accent.get(), .3f));
    }
    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void nexvisuals$before(Slot slot, int id, int button, ClickType type, CallbackInfo ci) {
        if (NexVisualsClient.instance() != null) nexvisuals$visuals.beforeClick(menu, slot, type, NexVisualsClient.instance().containers());
    }
    @Inject(method = "slotClicked", at = @At("RETURN"))
    private void nexvisuals$after(Slot slot, int id, int button, ClickType type, CallbackInfo ci) { nexvisuals$visuals.afterClick(menu); }
}
