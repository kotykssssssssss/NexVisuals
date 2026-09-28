package dev.nexvisuals.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class ClippedEditBox extends EditBox {
    private final GuiLayout.Rect clip;

    ClippedEditBox(Font font, int x, int y, int width, Component title, GuiLayout.Rect clip) {
        super(font, x, y, width, 20, title);
        this.clip = clip;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return clip.contains(event.x(), event.y()) && super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isMouseOver(double x, double y) {
        return clip.contains(x, y) && super.isMouseOver(x, y);
    }
}
