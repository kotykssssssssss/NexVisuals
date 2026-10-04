package dev.nexvisuals.client.post;

import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;

public final class WeatherLensModule extends VisualModule {
    public enum Style { DROPLETS, RIVULETS, MIST }
    public final EnumSetting<Style> style=add(new EnumSetting<>("style","Pattern","Round lens drops, elongated streams or fine mist. Only real exposed rain activates it.",Style.DROPLETS,Style.class));
    public final DoubleSetting intensity=number("intensity","Intensity",.5,0,1);
    public final DoubleSetting density=number("density","Drop density",8,4,20);
    public final DoubleSetting speed=number("speed","Fall speed",.5,.1,1.5);
    public final DoubleSetting refraction=number("refraction","Refraction (pixels)",2,0,5);
    public final DoubleSetting shading=number("shading","Droplet highlights",.25,0,.5);
    public final DoubleSetting transition=number("transition","Dry / wet transition (seconds)",.65,.1,2);
    public final DoubleSetting dropSize=number("drop_size","Droplet size",1,.6,1.7);
    public final DoubleSetting randomness=number("drop_randomness","Droplet placement variation",1,0,1);
    public final ColorSetting dropTint=add(new ColorSetting("drop_tint","Droplet tint","ARGB tint of the subtle lens shading only. Alpha zero preserves the original optics; scene lighting/fog are unaffected.",0x007EA3C4));
    private java.util.function.Supplier<String> status=()->"Waiting for world renderer.";
    public WeatherLensModule() {
        super("weather_lens","Weather Lens","Procedural rain on the lens with bounded image refraction. No weather replacement; no HUD distortion.",Category.SHADERS);
        group("Rain pattern",style,intensity,density,speed);
        group("Optics",refraction,shading,transition);
        group("Droplet appearance",dropSize,randomness,dropTint);
        preset("Drizzle","Small slow droplets with restrained refraction.");
        preset("Storm Glass","Faster narrow streams with stronger glass shading.","style","RIVULETS","density",12,"intensity",.75,"refraction",3.5,"speed",1,"shading",.4);
        preset("Mist","Fine small droplets with nearly no refraction.","style","MIST","density",18,"intensity",.4,"refraction",.6,"shading",.18,"speed",.2);
        preset("Pearl Glass","Larger, gentle droplets with slow motion and cool highlights.","drop_size",1.25,"density",7,"speed",.3,"drop_tint","#207EA3C4","refraction",1.6,"shading",.32);
    }
    private DoubleSetting number(String id,String name,double value,double min,double max) { return add(new DoubleSetting(id,name,"World-image effect; active only when the camera is exposed to actual rain.",value,min,max)); }
    void status(java.util.function.Supplier<String> source) { status=source; }
    @Override public String runtimeStatus() { return enabled()?status.get():"Disabled; rain and vanilla weather remain unchanged."; }
}
