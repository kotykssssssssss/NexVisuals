package dev.nexvisuals.client.hud;

import dev.nexvisuals.client.gui.HudEditorScreen;
import dev.nexvisuals.client.module.HudModule;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.hud.*;
import dev.nexvisuals.core.animation.Smoothing;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/** Wraps supported Fabric HUD layers, retaining their native data, drawing and visibility conditions. */
public final class VanillaHudModule extends VisualModule implements HudModule, EditableHud {
    public enum Element {
        HOTBAR(VanillaHudElements.HOTBAR, "Hotbar", 182, 22), HEALTH(VanillaHudElements.HEALTH_BAR, "Health", 81, 10),
        ARMOR(VanillaHudElements.ARMOR_BAR, "Armor", 81, 10), HUNGER(VanillaHudElements.FOOD_BAR, "Hunger", 81, 10),
        EXPERIENCE(VanillaHudElements.INFO_BAR, "Experience / info bar", 182, 5), LEVEL(VanillaHudElements.EXPERIENCE_LEVEL, "Experience level", 30, 10),
        EFFECTS(VanillaHudElements.STATUS_EFFECTS, "Status effects", 150, 56), BOSS(VanillaHudElements.BOSS_BAR, "Boss bars", 182, 48);
        final Identifier layer; final String label; final int width, height;
        Element(Identifier layer, String label, int width, int height) { this.layer=layer; this.label=label; this.width=width; this.height=height; }
    }
    private final Element element;
    private final BooleanSetting visible = add(new BooleanSetting("visible", "Visible", "Hide only this vanilla HUD layer while its customization is enabled.", true));
    private final DoubleSetting scale = add(new DoubleSetting("scale", "Scale", "Bottom HUD scales around the screen's bottom center. Linked Mini HUD uses the Hotbar scale.", 1, .5, 1.75));
    private final IntSetting x = add(new IntSetting("x", "Offset X", "GUI-pixel offset from the default position.", 0, -600, 600));
    private final IntSetting y = add(new IntSetting("y", "Offset Y", "GUI-pixel offset from the default position.", 0, -400, 400));
    private final BooleanSetting relative = add(new BooleanSetting("relative", "Use editor position", "HUD Editor stores a normalized position so resizing the window keeps it on screen.", false));
    private final DoubleSetting rx = add(new DoubleSetting("relative_x", "Relative X", "Normalized top-left X when editor positioning is enabled.", .4, 0, 1));
    private final DoubleSetting ry = add(new DoubleSetting("relative_y", "Relative Y", "Normalized top-left Y when editor positioning is enabled.", .8, 0, 1));
    private final BooleanSetting background = add(new BooleanSetting("background", "Background", "Additional translucent backplate. Does not recolor vanilla sprites.", false));
    private final ColorSetting plate = add(new ColorSetting("plate", "Background color", "ARGB backplate opacity and color.", 0x88121D30));
    private final BooleanSetting outline = add(new BooleanSetting("outline", "Outline", "Outline the approximate bounds of this layer.", false));
    private final ColorSetting accent = add(new ColorSetting("accent", "Outline color", "ARGB outline tint.", 0xAA8CD8FF));
    private HudElement original;
    private final BooleanSetting selectedOutline;
    private final BooleanSetting linked;
    private final VanillaHudModule hotbar;
    private final java.util.List<HudElement> companions = new java.util.ArrayList<>(3);
    private final java.util.List<VanillaHudModule> members = new java.util.ArrayList<>(5);
    private double selectedX;
    private long selectedNanos;
    public VanillaHudModule(Element element, VanillaHudModule hotbar) {
        super("vanilla_" + element.name().toLowerCase(java.util.Locale.ROOT), element == Element.HOTBAR ? "Hotbar / Mini HUD" : "Vanilla " + element.label,
                "Position and frame the original " + element.label + " layer. Disabling customization restores vanilla.", Category.HUD);
        this.element = element;
        this.hotbar = hotbar;
        if (hotbar != null && isBottomLayer()) hotbar.members.add(this);
        linked = element == Element.HOTBAR ? add(new BooleanSetting("linked", "Linked Mini HUD",
                "Scale and move hotbar, health, armor, hunger, air, mount health, experience and item name together. Overrides their separate positions/scales; turn off for independent editing.", true)) : null;
        selectedOutline = element == Element.HOTBAR ? add(new BooleanSetting("selected_outline", "Smooth selected slot",
                "Additional accent outline follows your selected slot with a short cosmetic transition.", false)) : null;
        preset("Vanilla", "Identity drawing and default position.");
        preset("Compact", "Small original HUD with a dark backplate.", "scale", .8, "background", true);
        if (element == Element.HOTBAR) preset("Mini", "One centered compact HUD, including health, food, experience and offhand.", "scale", .8, "selected_outline", true);
        if (element == Element.HOTBAR) preset("Framed", "Original HUD with frame and a smoothly moving selected-slot accent.", "background", true, "outline", true, "selected_outline", true);
        else preset("Framed", "Original-size HUD with accent border.", "background", true, "outline", true);
    }
    public boolean linkedLayout() { return element == Element.HOTBAR && enabled() && linked.get(); }
    public boolean followsHotbar() { return isBottomLayer() && hotbar != null && hotbar.linkedLayout(); }
    private boolean isBottomLayer() { return element != Element.EFFECTS && element != Element.BOSS; }
    @Override public boolean isHudActive(boolean enabled) { return enabled || followsHotbar(); }
    @Override public boolean isVisibleInEditor(Minecraft client) { return !followsHotbar(); }

    /** These auxiliary layers need the same transform; their original visibility/data remain vanilla. */
    public void registerCompanionLayers() {
        if (element != Element.HOTBAR) return;
        for (Identifier id : new Identifier[] { VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.HELD_ITEM_TOOLTIP }) {
            HudElementRegistry.replaceElement(id, layer -> {
                companions.add(layer);
                return (g, delta) -> {
                    if (!linkedLayout()) { layer.render(g, delta); return; }
                    if (Minecraft.getInstance().screen instanceof HudEditorScreen) return;
                    renderTransformed(g, delta, layer, transform(g.guiWidth(), g.guiHeight()));
                };
            });
        }
    }
    @Override public Identifier replacementLayer() { return element.layer; }
    @Override public boolean renderHud(Minecraft client, GuiGraphics g, DeltaTracker delta) { return false; }
    @Override public void renderWrapped(Minecraft client, GuiGraphics g, DeltaTracker delta, HudElement original) {
        this.original = original;
        if (client.screen instanceof HudEditorScreen || (enabled() && !visible.get())) return;
        draw(client, g, delta, original);
    }
    private Bounds vanillaBounds(int width, int height) {
        int left = width / 2 - element.width / 2, top = height - 22;
        switch (element) {
            case HEALTH -> { left = width/2-91; top = height-39; }
            case ARMOR -> { left = width/2-91; top = height-49; }
            case HUNGER -> { left = width/2+10; top = height-39; }
            case EXPERIENCE -> top = height-29;
            case LEVEL -> top = height-35;
            case EFFECTS -> { left = width-element.width-1; top = 1; }
            case BOSS -> top = 12;
            default -> { }
        }
        return new Bounds(left, top, element.width, element.height);
    }
    private Bounds layoutBounds(int width, int height) {
        // Reserve both offhand sides, mount/air rows and the selected item label in the group editor.
        return linkedLayout() ? new Bounds(width/2-121, height-80, 242, 80) : vanillaBounds(width, height);
    }
    public HudTransform transform(int width, int height) {
        if (followsHotbar()) return hotbar.transform(width, height);
        Bounds base = layoutBounds(width, height);
        double s = scale.get();
        if (relative.get()) {
            int w = (int) Math.ceil(base.width()*s), h = (int) Math.ceil(base.height()*s);
            var position = new HudPosition(rx.get(), ry.get(), Anchor.TOP_LEFT).resolve(width, height, w, h);
            return HudTransform.placed(s, base.x(), base.y(), position.x(), position.y());
        }
        HudTransform result = isBottomLayer() ? HudTransform.bottomCenter(width, height, s, x.get(), y.get())
                : HudTransform.placed(s, base.x(), base.y(), base.x()+x.get(), base.y()+y.get());
        return result.clamped(width, height, base.x(), base.y(), base.width(), base.height());
    }
    private static Bounds transformedBounds(Bounds base, HudTransform transform) {
        return new Bounds((int) Math.round(transform.x(base.x())), (int) Math.round(transform.y(base.y())),
                (int) Math.ceil(base.width()*transform.scale()), (int) Math.ceil(base.height()*transform.scale()));
    }
    @Override public Bounds bounds(Minecraft client, int width, int height) {
        return transformedBounds(layoutBounds(width, height), transform(width, height));
    }
    private static void applyTransform(GuiGraphics g, HudTransform transform) {
        g.pose().translate((float) transform.translateX(), (float) transform.translateY());
        g.pose().scale((float) transform.scale(), (float) transform.scale());
    }
    private static void renderTransformed(GuiGraphics g, DeltaTracker delta, HudElement layer, HudTransform transform) {
        g.pose().pushMatrix();
        try { applyTransform(g, transform); layer.render(g, delta); }
        finally { g.pose().popMatrix(); }
    }
    private void draw(Minecraft client, GuiGraphics g, DeltaTracker delta, HudElement layer) {
        HudTransform transform = transform(g.guiWidth(), g.guiHeight());
        Bounds target = transformedBounds(vanillaBounds(g.guiWidth(), g.guiHeight()), transform);
        if (enabled() && background.get()) Draw.roundedRect(g, target.x()-2, target.y()-2, target.width()+4, target.height()+4, 3, plate.get());
        g.pose().pushMatrix();
        applyTransform(g, transform);
        try {
            layer.render(g, delta);
            if (selectedOutline != null && selectedOutline.get() && client.player != null && !client.player.isSpectator()) {
                double targetX = g.guiWidth()/2 - 92 + client.player.getInventory().getSelectedSlot()*20;
                long now = System.nanoTime();
                selectedX = selectedNanos == 0 ? targetX : Smoothing.approach(selectedX, targetX, (now-selectedNanos)/1_000_000.0, 55);
                selectedNanos = now;
                Draw.border(g, (int) Math.round(selectedX), g.guiHeight()-23, 24, 23, 1, accent.get());
            }
        } finally { g.pose().popMatrix(); }
        if (enabled() && outline.get()) Draw.border(g, target.x()-2, target.y()-2, target.width()+4, target.height()+4, 1, accent.get());
    }
    @Override public void moveTo(int left, int top, int width, int height) {
        Bounds b = bounds(null, width, height);
        var position = HudPosition.fromTopLeft(left, top, width, height, b.width(), b.height(), Anchor.TOP_LEFT);
        rx.set(position.x()); ry.set(position.y()); relative.set(true);
    }
    @Override public void renderPreview(Minecraft client, GuiGraphics g) {
        if (original != null) draw(client, g, client.getDeltaTracker(), original);
        else {
            Bounds b = bounds(client, g.guiWidth(), g.guiHeight());
            Draw.rect(g, b.x(), b.y(), b.width(), b.height(), 0x66121D30);
            Draw.text(g, client.font, element.label, b.x()+2, b.y()+1, 0xFFDCE6F9, true);
        }
        if (linkedLayout()) {
            // One group preview and drag target instead of overlapping child handles.
            for (VanillaHudModule member : members) if (!member.enabled() || member.visible.get()) member.renderPreview(client, g);
            for (HudElement layer : companions) renderTransformed(g, client.getDeltaTracker(), layer, transform(g.guiWidth(), g.guiHeight()));
        }
    }
    @Override protected void onDisable() { selectedNanos = 0; }
}
