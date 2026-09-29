package dev.nexvisuals.core.hud;

import java.util.ArrayList;
import java.util.List;

/** Pixel runs are built only when geometry changes, not every rendered frame. */
public final class ReticleMask {
    public enum Shape { CIRCLE, CHEVRON }
    public record Span(int x, int y, int width, boolean outline) { }
    private ReticleMask() { }
    public static List<Span> build(Shape shape, int width, int height, int thickness, int gap, int border) {
        int w = Math.clamp(width, 0, 24), h = Math.clamp(height, 0, 24), t = Math.clamp(thickness, 1, 8);
        int g = Math.clamp(gap, 0, 16), b = Math.clamp(border, 0, 4);
        int rx = Math.max(1, w + (shape == Shape.CIRCLE ? g : 0));
        int ry = Math.max(1, h + (shape == Shape.CIRCLE ? g : 0));
        int extent = Math.max(rx, ry) + t + b + 2, size = extent * 2 + 1;
        boolean[][] fill = new boolean[size][size];
        for (int y=-extent; y<=extent; y++) for (int x=-extent; x<=extent; x++) {
            if (shape == Shape.CIRCLE) {
                double outer = (double)x*x/(rx*rx) + (double)y*y/(ry*ry);
                int ix = Math.max(0, rx-t), iy = Math.max(0, ry-t);
                double inner = ix == 0 || iy == 0 ? 2 : (double)x*x/(ix*ix) + (double)y*y/(iy*iy);
                fill[y+extent][x+extent] = outer <= 1 && inner >= 1;
            } else {
                double line = Math.abs(x) * (double) ry / rx - ry / 2.0;
                fill[y+extent][x+extent] = Math.abs(x) <= rx && Math.abs(x) >= g && Math.abs(y-line) <= t*.5;
            }
        }
        List<Span> spans = new ArrayList<>();
        for (int layer=0; layer<2; layer++) for (int y=0; y<size; y++) {
            int start=-1;
            for (int x=0; x<=size; x++) {
                boolean pixel=false;
                if (x<size) {
                    if (layer==1) pixel=fill[y][x];
                    else if (b>0 && !fill[y][x]) {
                        for (int dy=-b; dy<=b && !pixel; dy++) for (int dx=-b; dx<=b; dx++) {
                            if (y+dy>=0 && y+dy<size && x+dx>=0 && x+dx<size && fill[y+dy][x+dx]) { pixel=true; break; }
                        }
                    }
                }
                if (pixel && start<0) start=x;
                if (!pixel && start>=0) { spans.add(new Span(start-extent,y-extent,x-start,layer==0)); start=-1; }
            }
        }
        return List.copyOf(spans);
    }
}
