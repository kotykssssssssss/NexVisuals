package dev.nexvisuals.client.interfacefx;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;

/** Shared settings for every ordinary AbstractContainerScreen. Slot logic remains vanilla. */
public final class ContainerVisualsModule extends VisualModule {
    public enum Opening { FADE, SCALE, SLIDE, NONE }
    public enum Motion { SLIDE, SMOOTH, ARC, POP, FADE }
    public final ColorSetting panel = add(new ColorSetting("panel", "Panel tint", "Translucent tint over the vanilla panel, below its items and labels.", 0x30283D62));
    public final ColorSetting accent = add(new ColorSetting("accent", "Accent", "Outline, hover and click feedback ARGB.", 0xB87EDCFF));
    public final BooleanSetting outline = add(new BooleanSetting("outline", "Panel outline", "Decorative frame around the ordinary container.", true));
    public final IntSetting rounding = add(new IntSetting("rounding", "Frame rounding", "Rounded cosmetic frame; vanilla textures and slots remain rectangular.", 5, 0, 12));
    public final EnumSetting<Opening> opening = add(new EnumSetting<>("opening", "Opening frame", "Animate only the decorative frame. Actual slots always stay aligned with the mouse.", Opening.SCALE, Opening.class));
    public final IntSetting duration = add(new IntSetting("duration", "Animation duration", "Milliseconds. Inventory actions are never delayed.", 180, 60, 450));
    public final EnumSetting<Easing> easing = add(new EnumSetting<>("easing", "Easing", "Opening, hover and transfer curve.", Easing.OUT_CUBIC, Easing.class));
    public final BooleanSetting hover = add(new BooleanSetting("hover", "Smooth slot hover", "Smoothly fade the last few hovered slots.", true));
    public final ColorSetting slotTint = add(new ColorSetting("slot_tint", "Slot background tint", "ARGB fill behind slot items. Transparent alpha disables it.", 0x1424314B));
    public final BooleanSetting slotOutline = add(new BooleanSetting("slot_outline", "Slot outlines", "Subtle individual outlines for active slots.", false));
    public final BooleanSetting clicks = add(new BooleanSetting("clicks", "Click reactions", "Short outline/pop for pickup, drop, right-click and quick craft.", true));
    public final BooleanSetting transfers = add(new BooleanSetting("transfers", "Quick-move animation", "Visual ghost only when the local predicted slot changes identify a complete transfer.", true));
    public final EnumSetting<Motion> motion = add(new EnumSetting<>("motion", "Transfer style", "Uncertain destinations fall back to source-slot feedback.", Motion.ARC, Motion.class));
    public final DoubleSetting scale = add(new DoubleSetting("scale", "Ghost scale", "Scale of the cosmetic item copy only.", 1, .5, 1.5));
    public final DoubleSetting intensity = add(new DoubleSetting("intensity", "Motion intensity", "Arc height and pop strength.", 1, .1, 2));
    public final BooleanSetting afterimage = add(new BooleanSetting("afterimage", "Ghost outline", "A fading accent outline around the moving icon.", false));
    public ContainerVisualsModule() {
        super("container_visuals", "Container Visuals", "Shared panel, hover, click and observed quick-move feedback. No inventory automation.", Category.INTERFACE);
        preset("Clean", "Subtle blue tint and fast slot reactions.", "opening", "FADE", "motion", "SMOOTH", "duration", 140);
        preset("Aurora", "A violet frame with arc transfers.", "panel", "#383D284F", "accent", "#C0B299FF", "opening", "SCALE", "motion", "ARC");
        preset("Snap", "Short sliding frame and pop reactions.", "opening", "SLIDE", "motion", "POP", "duration", 100);
    }
}
