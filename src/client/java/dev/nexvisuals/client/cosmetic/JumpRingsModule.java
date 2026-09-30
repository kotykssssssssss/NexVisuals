package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.GroundMotion;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

public final class JumpRingsModule extends VisualModule {
    public enum Style { HALO, DOUBLE, RUNES }
    private final EnumSetting<Style> style=add(new EnumSetting<>("style","Shape","A single wave, two concentric rings, or a ring with star-shaped runes.",Style.DOUBLE,Style.class));
    private final BooleanSetting jump=add(new BooleanSetting("jump","On jump","Only an observed upward takeoff from the ground.",true));
    private final BooleanSetting landing=add(new BooleanSetting("landing","On landing","Wave size reacts to observed descent; no fall damage changes.",true));
    private final ColorSetting primary=add(new ColorSetting("primary","Primary","ARGB with opacity.",0xD976D9FF));
    private final ColorSetting secondary=add(new ColorSetting("secondary","Secondary","End color of the fading wave.",0x60C3A0FF));
    private final DoubleSetting radius=add(new DoubleSetting("radius","Radius","The expanding ring's approximate final radius in blocks.",1.3,.3,3));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Ticks; one or two quads plus at most eight runes per event.",22,8,50));
    private final GroundMotion motion=new GroundMotion();
    private final EffectEmitter emitter;
    private final BooleanSupplier active=this::enabled;
    private Object level;
    public JumpRingsModule(EffectEmitter emitter) {
        super("jump_rings","Jump / Landing Rings","Expanding waves fixed to the local takeoff/landing surface, with ordinary world depth.",Category.PARTICLES);
        this.emitter=emitter;
        preset("Double Halo","Two staggered cyan waves.");
        preset("Runic","A violet circle surrounded by eight rotating star marks.","style","RUNES","primary","#DDC299FF","secondary","#5577D6FF","lifetime",30);
        preset("Quiet","One small, short, pale wave.","style","HALO","radius",.8,"primary","#99FFFFFF","lifetime",14);
    }
    public void tick(Minecraft client) {
        if (!enabled() || client.level==null || client.player==null || client.isPaused() || client.player.isSpectator()
                || client.player.isInWater() || client.player.getAbilities().flying || client.player.isFallFlying()) { clear(); return; }
        if (level!=client.level) { motion.reset(); level=client.level; }
        var p=client.player;
        var event=motion.sample(p.getX(),p.getY(),p.getZ(),p.onGround());
        if (event==GroundMotion.Event.NONE || event==GroundMotion.Event.JUMP&&!jump.get() || event==GroundMotion.Event.LAND&&!landing.get()) return;
        // On takeoff the feet have already risen; use the previous tick's grounded feet height.
        Vec3 point=new Vec3(p.getX(),event==GroundMotion.Event.JUMP?p.yo+.035:p.getY()+.035,p.getZ());
        float r=(float)(radius.get()*(event==GroundMotion.Event.LAND?.8+.4*motion.impact():1)/2);
        emitter.ground(client,point,EffectParticle.Shape.RING,r,primary.get(),secondary.get(),lifetime.get(),EffectParticle.Scaling.EXPAND,0,active);
        if (style.get()==Style.DOUBLE) emitter.ground(client,point.add(0,.012,0),EffectParticle.Shape.RING,r*.65f,secondary.get(),primary.get(),lifetime.get()+5,EffectParticle.Scaling.EXPAND,0,active);
        if (style.get()==Style.RUNES) for (int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            emitter.ground(client,point.add(Math.cos(a)*r,0,Math.sin(a)*r),EffectParticle.Shape.STAR,r*.17f,
                    secondary.get(),primary.get(),lifetime.get(),EffectParticle.Scaling.PULSE,(float)a,active);
        }
    }
    private void clear() { motion.reset(); level=null; }
    @Override protected void onDisable() { clear(); }
}
