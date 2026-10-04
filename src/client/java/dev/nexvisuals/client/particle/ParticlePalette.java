package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.visual.*;
import java.util.function.Consumer;

/** Color-only tuning for geometric ribbons; avoids exposing sprite physics on mesh effects. */
public final class ParticlePalette {
    public enum Mode { LEGACY, STATIC, GRADIENT, RAINBOW, THEME }
    public final EnumSetting<Mode> mode;
    public final DoubleSetting opacity,brightness,speed,saturation,value,hue;
    private final Setting<?>[] settings;
    private long revision=Long.MIN_VALUE;
    private ParticleTuning cached;
    public ParticlePalette(Consumer<Setting<?>> add) {
        mode=new EnumSetting<>("color_mode","Color mode","Legacy keeps the existing palette. Rainbow is opt-in; Theme follows the NexVisuals accent.",Mode.LEGACY,Mode.class);
        opacity=number("opacity","Opacity multiplier",1,0,1);
        brightness=number("brightness","Color brightness",1,.25,1.5);
        speed=number("rainbow_speed","Rainbow speed",.15,0,1);
        saturation=number("rainbow_saturation","Rainbow saturation",.65,0,1);
        value=number("rainbow_brightness","Rainbow brightness",1,.1,1);
        hue=number("hue_offset","Hue offset",0,0,1);
        settings=new Setting<?>[]{mode,opacity,brightness,speed,saturation,value,hue};
        for(var s:settings) add.accept(s);
        for(var s:new Setting<?>[]{speed,saturation,value,hue}) s.visibleWhen(()->mode.get()==Mode.RAINBOW);
    }
    private DoubleSetting number(String id,String label,double value,double min,double max) { return new DoubleSetting(id,label,"Live color tuning for this geometric effect.",value,min,max); }
    public Setting<?>[] settings() { return settings.clone(); }
    public int color(int primary,int secondary,double age,double seconds,int accent) {
        long sum=0;for(var s:settings) sum+=s.revision();
        if(cached==null || revision!=sum) {
            revision=sum;
            cached=new ParticleTuning(ParticleTuning.ColorMode.valueOf(mode.get().name()),speed.get(),saturation.get(),value.get(),hue.get(),
                    opacity.get(),brightness.get(),.94,1,0,0,1,0,0,0,false,0,.35,false,1,.15,0,1,64,false);
        }
        return ParticleMath.color(cached,primary,secondary,age,.5,seconds,accent);
    }
}
