package dev.nexvisuals.client.post;

import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;

public final class RetroDisplayModule extends VisualModule {
    public final DoubleSetting intensity=number("intensity","Intensity",1,0,1);
    public final IntSetting pixelSize=add(new IntSetting("pixel_size","Pixel grid (pixels)","One preserves the original resolution. HUD and menu text stay sharp.",3,1,10));
    public final IntSetting levels=add(new IntSetting("levels","Color levels","Quantization per RGB channel; 256 preserves full color.",24,4,256));
    public final DoubleSetting dither=number("dither","Ordered dither",.35,0,1);
    public final DoubleSetting scanlines=number("scanlines","CRT scanlines",.14,0,.4);
    public final IntSetting spacing=add(new IntSetting("spacing","Scanline spacing","Physical pixels per line, independent of GUI scale.",3,2,8));
    public final DoubleSetting phosphor=number("phosphor","RGB phosphor mask",.08,0,.3);
    private java.util.function.Supplier<String> status=()->"Waiting for world renderer.";
    public RetroDisplayModule() {
        super("retro_display","Retro Display","Actual pixel grid, posterization, ordered dithering and CRT mask. Complements grading; does not restyle UI text.",Category.SHADERS);
        group("Pixel art",intensity,pixelSize,levels,dither);
        group("CRT surface",scanlines,spacing,phosphor);
        preset("Console CRT","Light pixelation with scanlines and RGB phosphor.");
        preset("Pixel Adventure","Larger blocks, restricted colors and stable ordered dithering.","pixel_size",6,"levels",8,"dither",.65,"scanlines",0,"phosphor",0);
        preset("Clean CRT","Original image resolution and colors with only the CRT surface pattern.","pixel_size",1,"levels",256,"dither",0,"scanlines",.2,"phosphor",.16);
        preset("Soft Mosaic","Coarse image sampling without scanlines or quantization.","pixel_size",8,"levels",256,"dither",0,"scanlines",0,"phosphor",0,"intensity",.75);
    }
    private DoubleSetting number(String id,String name,double value,double min,double max) { return add(new DoubleSetting(id,name,"World image only; toggle this module independently of Lightweight Shaders.",value,min,max)); }
    void status(java.util.function.Supplier<String> source) { status=source; }
    @Override public String runtimeStatus() { return enabled()?status.get():"Disabled; the original world image is retained."; }
}
