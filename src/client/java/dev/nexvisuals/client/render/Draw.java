package dev.nexvisuals.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Small GUI primitives using Minecraft's render submission and scissor stack. Colors are ARGB. */
public final class Draw {
    private Draw() {
    }

    public static void rect(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        if (width > 0 && height > 0 && (argb >>> 24) != 0) {
            graphics.fill(x, y, x + width, y + height, argb);
        }
    }

    /** Pixel-aligned rounded corners, without shaders or overlapping translucent rectangles. */
    public static void roundedRect(GuiGraphics graphics, int x, int y, int width, int height,
                                   int radius, int argb) {
        if (width <= 0 || height <= 0 || (argb >>> 24) == 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        rect(graphics, x, y + r, width, height - r * 2, argb);
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            int inset = (int) Math.ceil(r - Math.sqrt(r * r - dy * dy));
            rect(graphics, x + inset, y + row, width - inset * 2, 1, argb);
            rect(graphics, x + inset, y + height - row - 1, width - inset * 2, 1, argb);
        }
    }

    /** Draw an inside border. Corner pixels are submitted once, preserving alpha. */
    public static void border(GuiGraphics graphics, int x, int y, int width, int height,
                              int thickness, int argb) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }
        int t = Math.min(thickness, (Math.min(width, height) + 1) / 2);
        rect(graphics, x, y, width, t, argb);
        rect(graphics, x, y + Math.max(t, height - t), width, Math.min(t, height - t), argb);
        rect(graphics, x, y + t, t, height - t * 2, argb);
        rect(graphics, x + Math.max(t, width - t), y + t,
                Math.min(t, width - t), height - t * 2, argb);
    }

    public static void text(GuiGraphics graphics, Font font, String text, int x, int y,
                            int argb, boolean shadow) {
        graphics.drawString(font, text, x, y, argb, shadow);
    }

    /** Small bounded pixel diamond, using the same native GUI fill submission as other feedback. */
    public static void diamondOutline(GuiGraphics g,int cx,int cy,int radius,int thickness,int argb) {
        int r=Math.clamp(radius,1,32),t=Math.clamp(thickness,1,3);
        if((argb>>>24)==0) return;
        for(int dy=-r;dy<=r;dy++) {
            int half=r-Math.abs(dy),span=Math.min(t,half+1);
            rect(g,cx-half,cy+dy,span,1,argb);
            if(half*2+1>span) rect(g,cx+Math.max(1,half-span+1),cy+dy,Math.min(span,half),1,argb);
        }
    }

    /** Multiply existing alpha, so fading a translucent panel does not make it opaque. */
    public static int withAlpha(int argb, float opacity) {
        float clamped = Float.isFinite(opacity) ? Math.max(0, Math.min(1, opacity)) : 0;
        int alpha = Math.round((argb >>> 24) * clamped);
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    /** Pair with unclip in a finally block; Minecraft intersects nested clips automatically. */
    public static void clip(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + Math.max(0, width), y + Math.max(0, height));
    }

    public static void unclip(GuiGraphics graphics) {
        graphics.disableScissor();
    }
}
