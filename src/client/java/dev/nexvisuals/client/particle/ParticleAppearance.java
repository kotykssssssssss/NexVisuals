package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.visual.ParticleTuning;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

/** Metadata reused by each owner; it adds controls to that module, not a second particle engine. */
public final class ParticleAppearance {
    public enum Kind { FREE, GROUND, AMBIENT }
    public enum Shape { PRESET, SOFT, SPARK, DOT, STAR, RING, STREAK, PIXEL, DIAMOND, HEART }
    public final EnumSetting<ParticleTuning.ColorMode> colorMode;
    public final EnumSetting<Shape> shape;
    public final DoubleSetting opacity,brightness,rainbowSpeed,saturation,rainbowBrightness,hue;
    public final DoubleSetting drag,velocity,gravity,scatter,randomness,sizeVariance,lifeVariance,velocityVariance;
    public final DoubleSetting fadeIn,fadeOut,startSize,endSize,rotation,glow,distance;
    public final BooleanSetting envelope,scale,distanceFade;
    private final List<Setting<?>> settings=new ArrayList<>();
    private long revision=Long.MIN_VALUE;
    private ParticleTuning cached;

    public ParticleAppearance(Consumer<Setting<?>> add,Kind kind) { this(add,kind,.94,1); }
    public ParticleAppearance(Consumer<Setting<?>> add,Kind kind,double defaultDrag,double defaultGlow) {
        colorMode=register(add,new EnumSetting<>("particle_color_mode","Color mode","Legacy keeps the original recipe; gradients interpolate over life.",ParticleTuning.ColorMode.LEGACY,ParticleTuning.ColorMode.class));
        shape=register(add,new EnumSetting<>("particle_shape","Particle shape","Preset preserves original component shapes; override decorative sprites only.",Shape.PRESET,Shape.class));
        opacity=number(add,"opacity","Opacity multiplier",1,0,1);
        brightness=number(add,"brightness","Color brightness",1,.25,1.5);
        rainbowSpeed=number(add,"rainbow_speed","Rainbow speed",.15,0,1);
        saturation=number(add,"rainbow_saturation","Rainbow saturation",.65,0,1);
        rainbowBrightness=number(add,"rainbow_brightness","Rainbow brightness",1,.1,1);
        hue=number(add,"hue_offset","Hue offset",0,0,1);
        for(var s:List.of(rainbowSpeed,saturation,rainbowBrightness,hue)) s.visibleWhen(()->colorMode.get()==ParticleTuning.ColorMode.RAINBOW);
        drag=number(add,"drag","Velocity retention / drag",defaultDrag,.5,1);
        velocity=number(add,"velocity","Velocity multiplier",1,0,3);
        gravity=number(add,"gravity","Gravity offset",0,-1,1);
        scatter=number(add,"scatter","Extra velocity spread",0,0,.15);
        randomness=number(add,"randomness","Pattern randomness",1,0,1);
        sizeVariance=number(add,"size_variance","Size variance",0,0,.5);
        lifeVariance=number(add,"lifetime_variance","Lifetime variance",0,0,.5);
        velocityVariance=number(add,"velocity_variance","Velocity variance",0,0,.5);
        envelope=register(add,new BooleanSetting("particle_envelope","Custom fade windows","Keep legacy fading, or set separate fade-in/fade-out portions of life.",false));
        fadeIn=number(add,"fade_in","Fade in portion",0,0,.45);
        fadeOut=number(add,"fade_out","Fade out portion",.35,0,1);
        fadeIn.visibleWhen(envelope::get);fadeOut.visibleWhen(envelope::get);
        scale=register(add,new BooleanSetting("particle_scale","Custom size curve","Override the original scale animation with start/end multipliers.",false));
        startSize=number(add,"start_size","Start size multiplier",1,.05,3);
        endSize=number(add,"end_size","End size multiplier",kind==Kind.GROUND?2:.15,.01,3);
        startSize.visibleWhen(scale::get);endSize.visibleWhen(scale::get);
        rotation=number(add,"rotation","Extra rotation (deg/s)",0,-180,180);
        rotation.visibleWhen(()->shape.get()!=Shape.SOFT);
        glow=number(add,"glow","Glow / light intensity",defaultGlow,0,1);
        distance=number(add,"max_distance","Maximum render distance",64,8,96);
        distanceFade=register(add,new BooleanSetting("particle_distance_fade","Distance fade","Fade smoothly in the outer 30% of the selected distance. Ordinary depth still applies.",false));
        if(kind!=Kind.FREE) for(var s:List.of(drag,velocity,gravity,scatter,velocityVariance)) s.visibleWhen(()->false);
    }
    private <S extends Setting<?>> S register(Consumer<Setting<?>> add,S setting) { settings.add(setting);add.accept(setting);return setting; }
    private DoubleSetting number(Consumer<Setting<?>> add,String id,String name,double value,double min,double max) {
        return register(add,new DoubleSetting("particle_"+id,name,"Cosmetic tuning. Neutral defaults retain existing presets; new values apply to newly emitted particles.",value,min,max));
    }
    public List<Setting<?>> settings() { return Collections.unmodifiableList(settings); }
    public Setting<?>[] colors() { return new Setting<?>[]{colorMode,opacity,brightness,rainbowSpeed,saturation,rainbowBrightness,hue,glow}; }
    public Setting<?>[] motion() { return new Setting<?>[]{drag,velocity,gravity,scatter,randomness,velocityVariance,rotation}; }
    public Setting<?>[] advanced() { return new Setting<?>[]{shape,sizeVariance,lifeVariance,envelope,fadeIn,fadeOut,scale,startSize,endSize,distance,distanceFade}; }
    public void movingWhen(BooleanSupplier condition) {
        for(var s:List.of(drag,velocity,gravity,scatter,velocityVariance)) s.visibleWhen(condition);
    }
    public void spritesWhen(BooleanSupplier condition) {
        for(var s:List.of(shape,sizeVariance,lifeVariance,envelope,scale,randomness,glow)) s.visibleWhen(condition);
        rotation.visibleWhen(()->condition.getAsBoolean()&&shape.get()!=Shape.SOFT);
        fadeIn.visibleWhen(()->condition.getAsBoolean()&&envelope.get());fadeOut.visibleWhen(()->condition.getAsBoolean()&&envelope.get());
        startSize.visibleWhen(()->condition.getAsBoolean()&&scale.get());endSize.visibleWhen(()->condition.getAsBoolean()&&scale.get());
        movingWhen(condition);
    }
    public ParticleTuning snapshot() {
        long sum=0;for(var s:settings) sum+=s.revision();
        if(cached==null || sum!=revision) {
            revision=sum;
            cached=new ParticleTuning(colorMode.get(),rainbowSpeed.get(),saturation.get(),rainbowBrightness.get(),hue.get(),opacity.get(),brightness.get(),
                    drag.get(),velocity.get(),gravity.get(),scatter.get(),randomness.get(),sizeVariance.get(),lifeVariance.get(),velocityVariance.get(),
                    envelope.get(),fadeIn.get(),fadeOut.get(),scale.get(),startSize.get(),endSize.get(),Math.toRadians(rotation.get())/20,
                    glow.get(),distance.get(),distanceFade.get());
        }
        return cached;
    }
    public EffectParticle.Shape sprite(EffectParticle.Shape original) {
        return switch(shape.get()) {
            case PRESET -> original;
            case SOFT -> EffectParticle.Shape.ORB;
            case SPARK -> EffectParticle.Shape.SPARK;
            case STREAK -> EffectParticle.Shape.STREAK;
            case DOT -> EffectParticle.Shape.DOT;
            case STAR -> EffectParticle.Shape.STAR;
            case RING -> EffectParticle.Shape.RING;
            case HEART -> EffectParticle.Shape.HEART;
            case PIXEL -> EffectParticle.Shape.PIXEL;
            case DIAMOND -> EffectParticle.Shape.DIAMOND;
        };
    }
    /** Explicit color modes use the owner's palette, rather than inherited alternating components. */
    public int componentColor(int configured,int legacy) { return colorMode.get()==ParticleTuning.ColorMode.LEGACY?legacy:configured; }
}
