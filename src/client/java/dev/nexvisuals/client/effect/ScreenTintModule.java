package dev.nexvisuals.client.effect;

import dev.nexvisuals.client.module.HudModule;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.ColorSetting;
import dev.nexvisuals.core.setting.IntSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Adds a cosmetic edge tint; does not remove vanilla hurt, fire, fog or other gameplay cues. */
public final class ScreenTintModule extends VisualModule implements HudModule {
    private final ColorSetting color = add(new ColorSetting("color", "Edge color", "ARGB edge tint. Keep alpha low for a subtle result.", 0x40395A88));
    private final IntSetting size = add(new IntSetting("size", "Edge width", "Width of the fading border in GUI pixels.", 36, 4, 100));
    public ScreenTintModule() { super("screen_tint", "Screen Edge Tint", "An additional cosmetic vignette with configurable color and opacity.", Category.WORLD); }
    @Override public boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (client.player == null || client.level == null) return false;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        int edge = Math.min(size.get(), Math.min(w, h) / 2);
        // Nested disjoint outlines avoid multiplying alpha at corners.
        for (int i = 0; i < edge; i++) {
            float opacity = 1 - (float) i / edge;
            Draw.border(graphics, i, i, w - i * 2, h - i * 2, 1, Draw.withAlpha(color.get(), opacity * opacity));
        }
        return true;
    }
}
