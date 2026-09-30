package dev.nexvisuals.core.hud;

/** GUI-pixel layout, shared by drawing, hit areas and headless geometry checks. */
public record ConsoleLayout(Rect panel, int logoY, int buttonHeight, int gap, int primaryRows) {
    public enum Alignment { LEFT, CENTER, RIGHT }
    public record Rect(int x, int y, int width, int height) { }

    public static ConsoleLayout of(int width, int height, Alignment alignment, int primaryRows) {
        if (width < 1 || height < 1 || primaryRows < 1) throw new IllegalArgumentException("Invalid menu layout");
        // The development title has a fourth primary button. Leave room for the 51px vanilla logo.
        int gap = height < 250 && primaryRows >= 4 ? 2 : height < 300 ? 3 : 4;
        int button = height < 300 ? 20 : 26;
        int panelWidth = Math.min(232, Math.max(120, width-24));
        int panelHeight = 23 + primaryRows*(button+gap) + 20+gap + 20 + 10;
        int x = switch(alignment) {
            case LEFT -> 28;
            case CENTER -> (width-panelWidth)/2;
            case RIGHT -> width-panelWidth-28;
        };
        x = Math.clamp(x, 6, Math.max(6,width-panelWidth-6));
        int top = Math.max(83, (height-panelHeight)/2+20);
        top = Math.min(top, Math.max(6,height-panelHeight-18));
        return new ConsoleLayout(new Rect(x, top, panelWidth, panelHeight), Math.max(4,top-70), button, gap, primaryRows);
    }
    public Rect primary(int row) {
        return new Rect(panel.x()+8, panel.y()+23+row*(buttonHeight+gap), panel.width()-16, buttonHeight);
    }
    public int utilityY() { return primary(primaryRows).y(); }
    public int customY() { return utilityY()+20+gap; }
    public int centerX() { return panel.x()+panel.width()/2; }
}
