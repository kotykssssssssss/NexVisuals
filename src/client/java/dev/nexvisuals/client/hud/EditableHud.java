package dev.nexvisuals.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Optional capability for movable HUD modules. Coordinates are Minecraft GUI pixels. */
public interface EditableHud {
    Bounds bounds(Minecraft client, int screenWidth, int screenHeight);

    /** Implementations convert the requested top-left point to their persistent anchor/offset settings. */
    void moveTo(int x, int y, int screenWidth, int screenHeight);

    /** Render editor content even when the module is disabled, using safe placeholders outside a world. */
    void renderPreview(Minecraft client, GuiGraphics graphics);

    default boolean isVisibleInEditor(Minecraft client) { return true; }

    record Bounds(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
