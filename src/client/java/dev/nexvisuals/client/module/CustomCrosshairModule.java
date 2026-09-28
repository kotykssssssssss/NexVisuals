package dev.nexvisuals.client.module;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.BooleanSetting;
import dev.nexvisuals.core.setting.ColorSetting;
import dev.nexvisuals.core.setting.IntSetting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.resources.Identifier;

/** A static cosmetic reticle. Target detection and attack timing remain entirely vanilla. */
public final class CustomCrosshairModule extends VisualModule implements HudModule {
    private final IntSetting size = add(new IntSetting(
            "size", "Size", "Length of each crosshair arm in GUI pixels.", 5, 1, 24));
    private final IntSetting thickness = add(new IntSetting(
            "thickness", "Thickness", "Thickness of each crosshair arm in GUI pixels.", 1, 1, 8));
    private final IntSetting gap = add(new IntSetting(
            "gap", "Gap", "Space between the center and each crosshair arm.", 3, 0, 16));
    private final ColorSetting color = add(new ColorSetting(
            "color", "Color", "Crosshair color, including opacity (alpha).", 0xFFE6F7FF));
    private final BooleanSetting outline = add(new BooleanSetting(
            "outline", "Outline", "Add a dark outline for visibility on bright surfaces.", true));

    public CustomCrosshairModule() {
        super("custom_crosshair", "Custom Crosshair", "A clean cosmetic crosshair with vanilla attack feedback.", Category.HUD);
    }

    @Override
    public Identifier replacementLayer() {
        return VanillaHudElements.CROSSHAIR;
    }

    @Override
    public boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (client.player == null || client.level == null || !client.options.getCameraType().isFirstPerson()
                || client.player.isSpectator() || client.player.isScoping()
                || client.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
            return false;
        }
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2;
        int arm = size.get();
        int thick = thickness.get();
        int space = gap.get();
        int start = -thick / 2;
        arm(graphics, x - space - arm, y + start, arm, thick);
        arm(graphics, x + space + (thick + 1) / 2, y + start, arm, thick);
        arm(graphics, x + start, y - space - arm, thick, arm);
        arm(graphics, x + start, y + space + (thick + 1) / 2, thick, arm);
        return true;
    }

    private void arm(GuiGraphics graphics, int x, int y, int width, int height) {
        if (outline.get()) {
            // Match outline opacity to the reticle, including a fully transparent color.
            Draw.border(graphics, x - 1, y - 1, width + 2, height + 2, 1, color.get() & 0xFF000000);
        }
        Draw.rect(graphics, x, y, width, height, color.get());
    }

    @Override
    public void renderWrapped(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker, HudElement original) {
        if (!renderHud(client, graphics, deltaTracker)) {
            original.render(graphics, deltaTracker);
            return;
        }
        // In 1.21.11 the vanilla reticle is 15px tall; the attack indicator begins at centerY + 9.
        // Keep vanilla's own indicator calculation and rendering, clipping only its old reticle.
        int top = graphics.guiHeight() / 2 + 8;
        Draw.clip(graphics, 0, top, graphics.guiWidth(), graphics.guiHeight() - top);
        try {
            original.render(graphics, deltaTracker);
        } finally {
            Draw.unclip(graphics);
        }
    }
}
