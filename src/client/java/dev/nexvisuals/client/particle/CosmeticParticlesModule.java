package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** Legacy generic sprite now shares admission, tuning and batching with all other world effects. */
public final class CosmeticParticlesModule extends VisualModule {
    private final IntSetting amount=add(new IntSetting("amount","Amount","Particles per local attack burst, within the common budget.",12,1,40));
    private final DoubleSetting size=add(new DoubleSetting("size","Size","Billboard half-size in world units.",.075,.01,.2));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Ticks; 20 ticks is one second.",14,2,60));
    private final ColorSetting color=add(new ColorSetting("color","Start color","ARGB particle tint and opacity.",0xDDA5C8FF));
    private final DoubleSetting spread=add(new DoubleSetting("spread","Velocity / spread","Maximum initial velocity per axis.",.09,0,.3));
    private final BooleanSetting fade=add(new BooleanSetting("fade","Fade out","Fade opacity over particle lifetime.",true));
    private final EnumSetting<Easing> easing=add(new EnumSetting<>("easing","Fade easing","Opacity interpolation curve.",Easing.OUT_QUAD,Easing.class));
    private final ColorSetting end=add(new ColorSetting("end_color","End color","Used by Gradient color mode; legacy keeps the original start tint.",0xAAADB8E8));
    public final ParticleAppearance appearance;
    private final EffectEmitter emitter;
    private long lastBurst;
    public CosmeticParticlesModule() { this(new EffectEmitter()); }
    public CosmeticParticlesModule(EffectEmitter emitter) {
        super("cosmetic_particles","Classic Particle Style","Generic sprites used by Hit Effects > CLASSIC, with optional shape/color/motion tuning.",Category.PARTICLES);
        this.emitter=emitter;
        group("General",amount,size,lifetime,color,end,spread,fade,easing);
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.FREE,.92,0);
        group("Particle colors",appearance.colors());group("Particle motion",appearance.motion());group("Particle advanced",appearance.advanced());
        preset("Classic Plus","More readable generic motes with the original fade.");
        preset("Pearl Dust","Soft cyan-violet gradient sprites.","particle_shape","SOFT","particle_color_mode","GRADIENT","particle_glow",.6,"particle_envelope",true,"particle_fade_in",.05,"particle_fade_out",.55);
        preset("Pixel Confetti","A compact deterministic square burst.","particle_shape","PIXEL","size",.065,"particle_randomness",0,"particle_rotation",60);
    }
    public void burst(Minecraft client,Vec3 position,BooleanSupplier ownerEnabled) {
        if(!enabled() || client.level==null) return;
        long now=System.nanoTime();if(now-lastBurst<50_000_000L) return;lastBurst=now;
        var random=client.level.random;
        double velocity=spread.get(),variation=appearance.randomness.get();
        BooleanSupplier active=()->enabled()&&ownerEnabled.getAsBoolean();
        for(int i=0;i<amount.get();i++) {
            double angle=i*2.39996323;
            double dx=(Math.cos(angle)*(1-variation)+(random.nextDouble()*2-1)*variation)*velocity;
            double dy=(.4*(1-variation)+(random.nextDouble()*2-.5)*variation)*velocity;
            double dz=(Math.sin(angle)*(1-variation)+(random.nextDouble()*2-1)*variation)*velocity;
            int secondary=appearance.colorMode.get()==dev.nexvisuals.core.visual.ParticleTuning.ColorMode.LEGACY?color.get():end.get();
            emitter.emit(client,position,dx,dy,dz,EffectParticle.Shape.VANILLA,size.get().floatValue(),color.get(),secondary,
                    lifetime.get(),0,fade.get(),true,EffectParticle.Scaling.CONSTANT,easing.get(),0,0,active,appearance);
        }
    }
}
