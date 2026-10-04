package dev.nexvisuals.client.post;

import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;

public final class UnderwaterEffectsModule extends VisualModule {
    public enum SpeckShape { SOFT, DIAMOND, STAR }
    public final DoubleSetting intensity=number("intensity","Intensity",.65,0,1);
    public final DoubleSetting wobble=number("wobble","Refraction (pixels)",1.6,0,5);
    public final DoubleSetting speed=number("speed","Water motion",.65,.1,2);
    public final DoubleSetting caustics=number("caustics","Caustic shimmer",.1,0,.25);
    public final DoubleSetting causticScale=number("caustic_scale","Shimmer scale",10,4,24);
    public final DoubleSetting silt=number("silt","Suspended specks",.08,0,.3);
    public final DoubleSetting transition=number("transition","Submerge transition (seconds)",.4,.1,1.5);
    public final DoubleSetting siltSize=number("silt_size","Speck size",1,.5,3);
    public final DoubleSetting siltDensity=number("silt_density","Speck density",1,.5,2);
    public final DoubleSetting siltSpeed=number("silt_speed","Speck drift speed",1,0,3);
    public final EnumSetting<SpeckShape> siltShape=add(new EnumSetting<>("silt_shape","Speck shape","Soft dots, diamond flecks or tiny star glints; procedural screen decoration, not world objects.",SpeckShape.SOFT,SpeckShape.class));
    public final ColorSetting siltColor=add(new ColorSetting("silt_color","Speck color","ARGB of the multiplicative speck layer; black scene pixels remain black.",0xFFFFFFFF));
    private java.util.function.Supplier<String> status=()->"Waiting for world renderer.";
    public UnderwaterEffectsModule() {
        super("underwater_effects","Underwater FX","Small image ripples, animated light patterns and suspended specks while the real camera is underwater. Vanilla water fog stays intact.",Category.SHADERS);
        group("Water motion",intensity,wobble,speed,transition);
        group("Surface shimmer",caustics,causticScale);
        group("Suspended specks",silt,siltSize,siltDensity,siltSpeed,siltShape,siltColor);
        for(var setting:new Setting<?>[]{siltSize,siltDensity,siltSpeed,siltShape,siltColor}) setting.visibleWhen(()->silt.get()>0);
        preset("Quiet Water","Restrained refraction and very soft shimmer.");
        preset("Lagoon","Visible fluid ripples and fine animated shimmer.","wobble",3,"caustics",.2,"caustic_scale",16,"silt",.05,"intensity",.8);
        preset("Deep","Slow refraction and more suspended specks, with no added shimmer.","wobble",2.2,"speed",.3,"caustics",0,"silt",.2);
        preset("Pearl Lagoon","Readable slowly drifting specks, subdued refraction and fine shimmer.","silt_size",1.45,"silt_density",.8,"silt_speed",.5,"silt",.14,"silt_color","#BBA4D8DF","wobble",1.3,"caustics",.12);
        preset("Crystal Depths","Sparse diamond-shaped suspended flecks without added caustics.","silt_shape","DIAMOND","silt_size",1.7,"silt_density",.7,"silt",.17,"silt_speed",.4,"silt_color","#DDD0C8EB","caustics",0,"speed",.4);
    }
    private DoubleSetting number(String id,String name,double value,double min,double max) { return add(new DoubleSetting(id,name,"A screen-space decoration, not physical water lighting or a visibility boost.",value,min,max)); }
    void status(java.util.function.Supplier<String> source) { status=source; }
    @Override public String runtimeStatus() { return enabled()?status.get():"Disabled; vanilla water rendering remains unchanged."; }
}
