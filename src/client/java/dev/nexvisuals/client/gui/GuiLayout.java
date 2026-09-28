package dev.nexvisuals.client.gui;

/** Logical GUI pixels, independent of the physical window size and Minecraft GUI scale. */
record GuiLayout(int left, int top, int width, int height, Rect categories, Rect modules,
                 Rect settings, int toolbarY, int footerY, boolean narrow) {
    static GuiLayout of(int screenWidth, int screenHeight, boolean details) {
        int margin = screenWidth < 420 ? 6 : 12;
        int width = Math.min(1040, screenWidth - margin * 2);
        int height = Math.min(650, screenHeight - margin * 2);
        int left = (screenWidth - width) / 2;
        int top = (screenHeight - height) / 2;
        int contentY = top + 72;
        int contentHeight = Math.max(32, height - 104);
        boolean narrow = width < 470;
        Rect categories = null;
        Rect modules;
        Rect settings;
        if (narrow) {
            Rect content = new Rect(left + 8, contentY, width - 16, contentHeight);
            modules = details ? null : content;
            settings = details ? content : null;
        } else {
            int categoryWidth = width >= 720 ? 108 : 0;
            if (categoryWidth > 0) {
                categories = new Rect(left + 8, contentY, categoryWidth, contentHeight);
            }
            int moduleX = left + 8 + (categoryWidth > 0 ? categoryWidth + 8 : 0);
            int moduleWidth = width < 580 ? 156 : 190;
            modules = new Rect(moduleX, contentY, moduleWidth, contentHeight);
            int settingX = moduleX + moduleWidth + 8;
            settings = new Rect(settingX, contentY, left + width - 8 - settingX, contentHeight);
        }
        return new GuiLayout(left, top, width, height, categories, modules, settings,
                top + 43, top + height - 25, narrow);
    }

    record Rect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
