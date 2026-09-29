package dev.nexvisuals.client.hud;

import dev.nexvisuals.client.gui.HudEditorScreen;
import dev.nexvisuals.client.module.HudModule;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.hud.Anchor;
import dev.nexvisuals.core.hud.HudPosition;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Shared placement, appearance and editor integration for small text HUD elements. */
public abstract class TextHudModule extends VisualModule implements HudModule, EditableHud {
    protected final EnumSetting<Anchor> anchor = add(new EnumSetting<>("anchor", "Anchor", "Element alignment.", Anchor.TOP_LEFT, Anchor.class));
    private final IntSetting xOffset;
    private final IntSetting yOffset;
    private final BooleanSetting relative = add(new BooleanSetting("relative_position", "Relative position", "The HUD editor enables this to preserve relative placement after resizing.", false));
    private final DoubleSetting relativeX = add(new DoubleSetting("relative_x", "Relative X", "Position across the screen (0 to 1).", .02, 0, 1));
    private final DoubleSetting relativeY = add(new DoubleSetting("relative_y", "Relative Y", "Position down the screen (0 to 1).", .15, 0, 1));
    private final DoubleSetting scale = add(new DoubleSetting("scale", "Scale", "Text and panel scale.", 1, .5, 3));
    private final ColorSetting color = add(new ColorSetting("color", "Text color", "ARGB including opacity.", 0xFFF1F5FF));
    private final BooleanSetting shadow = add(new BooleanSetting("shadow", "Text shadow", "Improve text readability.", true));
    private final BooleanSetting background = add(new BooleanSetting("background", "Background", "Draw a panel behind the text.", true));
    private final ColorSetting backgroundColor = add(new ColorSetting("background_color", "Background color", "ARGB panel color.", 0xA8151A26));
    private final IntSetting rounding = add(new IntSetting("rounding", "Rounding", "Panel corner radius in GUI pixels.", 3, 0, 8));
    private final String preview;
    protected String text = "";

    protected TextHudModule(String id, String name, String description, int defaultY, String preview) {
        super(id, name, description, Category.HUD);
        this.preview = preview;
        xOffset = add(new IntSetting("x", "X offset", "GUI pixels from the anchor when Relative position is disabled.", 8, 0, 4096));
        yOffset = add(new IntSetting("y", "Y offset", "GUI pixels from the anchor when Relative position is disabled.", defaultY, 0, 4096));
    }
    protected boolean canDisplay(Minecraft client) { return client.player != null && client.level != null; }
    protected final String displayedText() { return text.isEmpty() ? preview : text; }
    protected int contentWidth(Minecraft client) { return client.font.width(displayedText()); }
    protected int contentHeight(Minecraft client) { return client.font.lineHeight; }
    protected void drawContent(Minecraft client, GuiGraphics graphics, int color, boolean shadow) {
        Draw.text(graphics, client.font, displayedText(), 5, 4, color, shadow);
    }
    @Override public Bounds bounds(Minecraft client, int screenWidth, int screenHeight) {
        int width = (int) Math.ceil((contentWidth(client) + 10) * scale.get());
        int height = (int) Math.ceil((contentHeight(client) + 8) * scale.get());
        int x, y;
        if (relative.get()) {
            var point = new HudPosition(relativeX.get(), relativeY.get(), anchor.get()).resolve(screenWidth, screenHeight, width, height);
            x = point.x(); y = point.y();
        } else {
            x = (int) (anchor.get().x() * (screenWidth - width)) + (anchor.get().x() == 1 ? -xOffset.get() : xOffset.get());
            y = (int) (anchor.get().y() * (screenHeight - height)) + (anchor.get().y() == 1 ? -yOffset.get() : yOffset.get());
            x = Math.clamp(x, 0, Math.max(0, screenWidth - width));
            y = Math.clamp(y, 0, Math.max(0, screenHeight - height));
        }
        return new Bounds(x, y, width, height);
    }
    @Override public void moveTo(int x, int y, int screenWidth, int screenHeight) {
        Bounds current = bounds(Minecraft.getInstance(), screenWidth, screenHeight);
        HudPosition position = HudPosition.fromTopLeft(x, y, screenWidth, screenHeight, current.width(), current.height(), anchor.get());
        relativeX.set(position.x()); relativeY.set(position.y()); relative.set(true);
    }
    @Override public boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!canDisplay(client) || client.screen instanceof HudEditorScreen) return false;
        if (text.isEmpty()) tick(client);
        if (text.isEmpty()) return false;
        renderPreview(client, graphics);
        return true;
    }
    @Override public void renderPreview(Minecraft client, GuiGraphics graphics) {
        Bounds bounds = bounds(client, graphics.guiWidth(), graphics.guiHeight());
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate((float) bounds.x(), (float) bounds.y());
            graphics.pose().scale(scale.get().floatValue(), scale.get().floatValue());
            int width = contentWidth(client) + 10;
            if (background.get()) Draw.roundedRect(graphics, 0, 0, width, contentHeight(client) + 8, rounding.get(), backgroundColor.get());
            drawContent(client, graphics, color.get(), shadow.get());
        } finally { graphics.pose().popMatrix(); }
    }
    @Override protected void onDisable() { text = ""; }
}
