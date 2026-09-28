package dev.nexvisuals.client.module;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.BooleanSetting;
import dev.nexvisuals.core.setting.ColorSetting;
import dev.nexvisuals.core.setting.DoubleSetting;
import dev.nexvisuals.core.setting.EnumSetting;
import dev.nexvisuals.core.setting.IntSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Displays only the local player's block position, with no entity or world scanning. */
public final class HudCoordinatesModule extends VisualModule implements HudModule {
    private final EnumSetting<Anchor> anchor = add(new EnumSetting<>(
            "anchor", "Anchor", "Screen corner used for the position offsets.", Anchor.TOP_LEFT, Anchor.class));
    private final IntSetting xOffset = add(new IntSetting(
            "x", "X offset", "Horizontal distance from the selected corner in GUI pixels.", 8, 0, 4096));
    private final IntSetting yOffset = add(new IntSetting(
            "y", "Y offset", "Vertical distance from the selected corner in GUI pixels.", 20, 0, 4096));
    private final DoubleSetting scale = add(new DoubleSetting(
            "scale", "Scale", "Visual text scale; position stays inside the screen.", 1.0, 0.5, 3.0));
    private final ColorSetting color = add(new ColorSetting(
            "color", "Text color", "Text color, including opacity (alpha).", 0xFFF1F5FF));
    private final BooleanSetting shadow = add(new BooleanSetting(
            "shadow", "Text shadow", "Use Minecraft's text shadow for readability.", true));
    private final BooleanSetting background = add(new BooleanSetting(
            "background", "Background", "Draw a small rounded panel behind the coordinates.", true));
    private final ColorSetting backgroundColor = add(new ColorSetting(
            "background_color", "Background color", "Panel color, including opacity (alpha).", 0xA8151A26));

    private String coordinates = "";
    private int lastX;
    private int lastY;
    private int lastZ;

    public HudCoordinatesModule() {
        super("hud_coordinates", "HUD Coordinates", "Your block position in a movable HUD panel.", Category.HUD);
    }

    @Override
    public void tick(Minecraft client) {
        if (client.player == null || client.player.isReducedDebugInfo()) {
            coordinates = "";
            return;
        }
        int x = client.player.getBlockX();
        int y = client.player.getBlockY();
        int z = client.player.getBlockZ();
        if (coordinates.isEmpty() || x != lastX || y != lastY || z != lastZ) {
            lastX = x;
            lastY = y;
            lastZ = z;
            // Text is rebuilt when the block position changes, never unconditionally per frame.
            coordinates = "XYZ: " + x + " / " + y + " / " + z;
        }
    }

    @Override
    public boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (client.player == null || client.level == null || client.player.isReducedDebugInfo()) {
            return false;
        }
        if (coordinates.isEmpty()) {
            tick(client);
        }
        float textScale = scale.get().floatValue();
        int panelWidth = client.font.width(coordinates) + 10;
        int panelHeight = client.font.lineHeight + 8;
        int width = (int) Math.ceil(panelWidth * textScale);
        int height = (int) Math.ceil(panelHeight * textScale);
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        Anchor corner = anchor.get();
        int x = corner.right ? screenWidth - xOffset.get() - width : xOffset.get();
        int y = corner.bottom ? screenHeight - yOffset.get() - height : yOffset.get();
        x = Math.clamp(x, 0, Math.max(0, screenWidth - width));
        y = Math.clamp(y, 0, Math.max(0, screenHeight - height));

        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate((float) x, (float) y);
            graphics.pose().scale(textScale, textScale);
            if (background.get()) {
                Draw.roundedRect(graphics, 0, 0, panelWidth, panelHeight, 3, backgroundColor.get());
            }
            Draw.text(graphics, client.font, coordinates, 5, 4, color.get(), shadow.get());
        } finally {
            graphics.pose().popMatrix();
        }
        return true;
    }

    public enum Anchor {
        TOP_LEFT("Top left", false, false),
        TOP_RIGHT("Top right", true, false),
        BOTTOM_LEFT("Bottom left", false, true),
        BOTTOM_RIGHT("Bottom right", true, true);

        private final String label;
        private final boolean right;
        private final boolean bottom;

        Anchor(String label, boolean right, boolean bottom) {
            this.label = label;
            this.right = right;
            this.bottom = bottom;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
