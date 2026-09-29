package dev.nexvisuals.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;

public final class FireOverlayModule extends VisualModule {
    private final BooleanSetting visible = add(new BooleanSetting("visible", "Visible", "Only first-person flames. Burning, damage and world fire are unchanged.", true));
    private final DoubleSetting height = number("height", "Height", 1, .15, 1.5);
    private final DoubleSetting width = number("width", "Width", 1, .4, 1.5);
    private final DoubleSetting x = number("x", "Horizontal offset", 0, -.8, .8);
    private final DoubleSetting y = number("y", "Vertical offset", 0, -.8, .8);
    private final DoubleSetting opacity = number("opacity", "Opacity", 1, 0, 1);
    public FireOverlayModule() {
        super("fire_overlay", "Fire Overlay", "Cosmetic first-person fire framing. Does not change whether the player is burning.", Category.INTERFACE);
        preset("Vanilla", "Original size, placement and alpha.");
        preset("Low", "Lower, shorter flames.", "height", .65, "y", -.25);
        preset("Minimal", "Small translucent flames along the bottom.", "height", .35, "y", -.4, "opacity", .55);
        preset("Hidden", "Hide only first-person fire quads.", "visible", false);
    }
    private DoubleSetting number(String id, String name, double value, double min, double max) { return add(new DoubleSetting(id, name, "First-person overlay rendering only.", value, min, max)); }
    public boolean hidden() { return enabled() && (!visible.get() || opacity.get() == 0); }
    public float alpha(float vanilla) { return enabled() ? vanilla * opacity.get().floatValue() : vanilla; }
    public void transform(PoseStack pose) { if (enabled()) { pose.translate(x.get(), y.get(), 0); pose.scale(width.get().floatValue(), height.get().floatValue(), 1); } }
}
