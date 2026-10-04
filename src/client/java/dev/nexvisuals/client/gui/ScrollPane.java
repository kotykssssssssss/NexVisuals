package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.render.Draw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;

/** Widgets keep their identity while scrolling, so text selection and edit drafts are retained. */
final class ScrollPane {
    GuiLayout.Rect area;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Decoration> decorations = new ArrayList<>();
    private int scroll;
    private int contentHeight;

    ScrollPane(GuiLayout.Rect area, int scroll) { this.area = area; this.scroll = scroll; }

    void add(AbstractWidget widget, int offset) {
        entries.add(new Entry(widget, offset));
        if (widget instanceof NexButton button) button.clipped(area);
        if (widget instanceof ModuleRow row) row.clipped(area);
        contentHeight = Math.max(contentHeight, offset + widget.getHeight() + 6);
        position();
    }

    void decorate(int offset, int height, Painter painter) {
        decorations.add(new Decoration(offset, height, painter));
        contentHeight = Math.max(contentHeight, offset + height + 6);
    }

    int scroll() { return scroll; }
    void moveTo(GuiLayout.Rect next) {
        int dx=next.x()-area.x(); area=next;
        for(Entry entry:entries) {
            entry.widget.setX(entry.widget.getX()+dx);
            if(entry.widget instanceof NexButton button) button.clipped(area);
            if(entry.widget instanceof ModuleRow row) row.clipped(area);
        }
        position();
    }
    AbstractWidget click(net.minecraft.client.input.MouseButtonEvent event,boolean twice) {
        if(!area.contains(event.x(),event.y())) return null;
        for(Entry entry:entries) if(entry.widget.mouseClicked(event,twice)) return entry.widget;
        return null;
    }
    void scrollBy(int amount) { scroll = Math.clamp(scroll + amount, 0, maxScroll()); position(); }
    private int maxScroll() { return Math.max(0, contentHeight - area.height()); }
    private void position() {
        for (Entry entry : entries) entry.widget.setY(area.y() + entry.offset - scroll);
    }

    void revealFocused() {
        for (Entry entry : entries) {
            if (!entry.widget.isFocused()) continue;
            if (entry.offset < scroll) scroll = entry.offset;
            if (entry.offset + entry.widget.getHeight() > scroll + area.height()) {
                scroll = entry.offset + entry.widget.getHeight() - area.height();
            }
            scroll = Math.clamp(scroll, 0, maxScroll());
            position();
            return;
        }
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        scroll = Math.clamp(scroll, 0, maxScroll());
        position();
        Draw.clip(graphics, area.x(), area.y(), area.width(), area.height());
        try {
            for (Decoration decoration : decorations) {
                int y = area.y() + decoration.offset - scroll;
                if (y + decoration.height > area.y() && y < area.bottom()) decoration.painter.draw(graphics, y);
            }
            for (Entry entry : entries) {
                AbstractWidget widget = entry.widget;
                if (widget.getBottom() <= area.y() || widget.getY() >= area.bottom()) continue;
                widget.render(graphics, area.contains(mouseX, mouseY) ? mouseX : -1000,
                        area.contains(mouseX, mouseY) ? mouseY : -1000, delta);
            }
        } finally {
            Draw.unclip(graphics);
        }
        if (maxScroll() > 0) {
            int thumbHeight = Math.max(12, area.height() * area.height() / contentHeight);
            int thumbY = area.y() + (area.height() - thumbHeight) * scroll / maxScroll();
            Draw.roundedRect(graphics, area.right() - 3, thumbY, 2, thumbHeight, 1, 0xFF63718D);
        }
    }

    private record Entry(AbstractWidget widget, int offset) { }
    private record Decoration(int offset, int height, Painter painter) { }
    @FunctionalInterface interface Painter { void draw(GuiGraphics graphics, int y); }
}
