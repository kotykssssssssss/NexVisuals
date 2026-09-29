package dev.nexvisuals.client.module;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.BooleanSetting;
import dev.nexvisuals.core.setting.ColorSetting;
import dev.nexvisuals.core.setting.IntSetting;
import dev.nexvisuals.core.setting.EnumSetting;
import dev.nexvisuals.core.hud.ReticleMask;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.resources.Identifier;

/** A static cosmetic reticle. Target detection and attack timing remain entirely vanilla. */
public final class CustomCrosshairModule extends VisualModule implements HudModule {
    public enum Shape { CROSS, DOT, CIRCLE, CHEVRON }
    private final EnumSetting<Shape> shape = add(new EnumSetting<>("shape", "Shape", "Different reticle geometry; never uses target or hidden entity data.", Shape.CROSS, Shape.class));
    private final IntSetting size = add(new IntSetting(
            "size", "Height", "Vertical arm length, ring radius or chevron height in GUI pixels.", 5, 0, 24));
    private final IntSetting width = add(new IntSetting("width", "Width", "Horizontal arm length, ring radius or chevron half-width.", 5, 0, 24));
    private final IntSetting thickness = add(new IntSetting(
            "thickness", "Thickness", "Thickness of each crosshair arm in GUI pixels.", 1, 1, 8));
    private final IntSetting gap = add(new IntSetting(
            "gap", "Gap", "Space between the center and each crosshair arm.", 3, 0, 16));
    private final ColorSetting color = add(new ColorSetting(
            "color", "Color", "Crosshair color, including opacity (alpha).", 0xFFE6F7FF));
    private final BooleanSetting outline = add(new BooleanSetting(
            "outline", "Outline", "Add a dark outline for visibility on bright surfaces.", true));
    private final IntSetting outlineThickness = add(new IntSetting("outline_thickness", "Outline thickness", "Outline width in GUI pixels.", 1, 1, 4));
    private final ColorSetting outlineColor = add(new ColorSetting("outline_color", "Outline color", "Independent ARGB outline color.", 0xFF000000));
    private final BooleanSetting centerDot = add(new BooleanSetting("center_dot", "Center dot", "Draw a centered square dot.", false));
    private final IntSetting dotSize = add(new IntSetting("dot_size", "Dot size", "Center dot diameter in GUI pixels.", 2, 1, 6));
    private long maskKey = Long.MIN_VALUE;
    private java.util.List<ReticleMask.Span> mask = java.util.List.of();

    public CustomCrosshairModule() {
        super("custom_crosshair", "Custom Crosshair", "A clean cosmetic crosshair with vanilla attack feedback.", Category.HUD);
        preset("Classic", "Thin open cross with a black outline.");
        preset("Dot", "A small cyan dot.", "shape", "DOT", "dot_size", 2, "color", "#FF72DFFF");
        preset("Wide", "Short vertical arms and longer horizontal arms.", "size", 3, "width", 8, "gap", 4);
        preset("Precision", "Tiny arms around a central point.", "size", 2, "width", 2, "gap", 2, "center_dot", true, "dot_size", 1, "color", "#FF71E6B5");
        preset("Orbit", "A circular reticle with a single-pixel center.", "shape", "CIRCLE", "size", 5, "width", 5, "gap", 1, "center_dot", true, "dot_size", 1, "color", "#FFB298FF");
        preset("Chevron", "An angular upward-pointing reticle.", "shape", "CHEVRON", "size", 5, "width", 6, "gap", 0, "color", "#FFFFD080");
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
        if (shape.get() == Shape.CROSS) {
            arm(graphics, x - space - thick / 2 - width.get(), y + start, width.get(), thick);
            arm(graphics, x + space + (thick + 1) / 2, y + start, width.get(), thick);
            arm(graphics, x + start, y - space - thick / 2 - arm, thick, arm);
            arm(graphics, x + start, y + space + (thick + 1) / 2, thick, arm);
        } else if (shape.get() != Shape.DOT) {
            long key = shape.get().ordinal() | (long)width.get()<<4 | (long)arm<<10 | (long)thick<<16
                    | (long)space<<20 | (long)outlineThickness.get()<<26 | (outline.get() ? 1L<<30 : 0);
            if (key != maskKey) {
                mask = ReticleMask.build(shape.get() == Shape.CIRCLE ? ReticleMask.Shape.CIRCLE : ReticleMask.Shape.CHEVRON,
                        width.get(), arm, thick, space, outline.get() ? outlineThickness.get() : 0);
                maskKey = key;
            }
            for (var span : mask) Draw.rect(graphics, x+span.x(), y+span.y(), span.width(), 1, span.outline() ? outlineColor.get() : color.get());
        }
        if (centerDot.get() || shape.get() == Shape.DOT) arm(graphics, x - dotSize.get() / 2, y - dotSize.get() / 2, dotSize.get(), dotSize.get());
        return true;
    }

    private void arm(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (outline.get()) {
            int t = outlineThickness.get();
            Draw.border(graphics, x - t, y - t, width + 2 * t, height + 2 * t, t, outlineColor.get());
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
