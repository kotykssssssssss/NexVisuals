package dev.nexvisuals.client.gui;

import java.util.ArrayList;
import java.util.List;

/** Responsive category windows; pagination keeps every category reachable at high GUI scales. */
record CategoryBoardLayout(GuiLayout.Rect workspace, List<GuiLayout.Rect> slots, GuiLayout.Rect inspector,
                           int pages, int page, int capacity) {
    static CategoryBoardLayout of(int width, int height, int categoryCount, int requestedPage) {
        int gap = 8, margin = width < 420 ? 6 : 12;
        var space = new GuiLayout.Rect(margin, 65, Math.max(32, width - margin * 2), Math.max(34, height - 97));
        int columns = Math.clamp((space.width() + gap) / 140, 1, 6);
        int availableRows = Math.max(1, (space.height() + gap) / 84);
        int capacity = columns * availableRows;
        int pages = Math.max(1, (categoryCount + capacity - 1) / capacity);
        int page = Math.clamp(requestedPage, 0, pages - 1);
        int count = Math.min(capacity, Math.max(0, categoryCount - page * capacity));
        int usedRows = Math.max(1, (count + columns - 1) / columns);
        int panelWidth = (space.width() - gap * (columns - 1)) / columns;
        int panelHeight = Math.min(240, (space.height() - gap * (usedRows - 1)) / usedRows);
        var slots = new ArrayList<GuiLayout.Rect>();
        for (int i = 0; i < count; i++) slots.add(new GuiLayout.Rect(space.x() + i % columns * (panelWidth + gap),
                space.y() + i / columns * (panelHeight + gap), panelWidth, panelHeight));
        int iw = Math.min(510, Math.max(64, width - margin * 2));
        int ih = Math.min(650, Math.max(72, height - margin * 2));
        return new CategoryBoardLayout(space, List.copyOf(slots), new GuiLayout.Rect((width-iw)/2,(height-ih)/2,iw,ih), pages, page, capacity);
    }
}
