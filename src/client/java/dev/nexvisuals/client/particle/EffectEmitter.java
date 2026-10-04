package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.visual.*;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** Shared admission, immutable style snapshots and vanilla atlas batching; no parallel engine. */
public final class EffectEmitter {
    private static final Identifier[] SPRITES=java.util.Arrays.stream(EffectParticle.Shape.values())
            .map(s->Identifier.fromNamespaceAndPath("nexvisuals",s.name().toLowerCase(java.util.Locale.ROOT))).toArray(Identifier[]::new);
    private static final Identifier VANILLA_SPRITE=Identifier.withDefaultNamespace("generic_0");
    private final ParticleBudget budget=new ParticleBudget();
    private GlobalSettings globals;
    private long sequence;
    public void configure(GlobalSettings globals) { this.globals=globals; }
    public ParticleBudget.Quality quality() { return globals==null?ParticleBudget.Quality.HIGH:globals.particleQuality.get(); }
    private int accent() { return globals==null?0xFF8B9DFF:globals.accentColor.get(); }

    public void emit(Minecraft client,Vec3 p,double dx,double dy,double dz,EffectParticle.Shape shape,float size,
            int primary,int secondary,int lifetime,float gravity,boolean fade,boolean glow,EffectParticle.Scaling scaling,
            Easing easing,float rotation,float spin,BooleanSupplier active) {
        emit(client,p,dx,dy,dz,shape,size,primary,secondary,lifetime,gravity,fade,glow,scaling,easing,rotation,spin,active,null);
    }
    public void emit(Minecraft client,Vec3 p,double dx,double dy,double dz,EffectParticle.Shape shape,float size,
            int primary,int secondary,int lifetime,float gravity,boolean fade,boolean glow,EffectParticle.Scaling scaling,
            Easing easing,float rotation,float spin,BooleanSupplier active,ParticleAppearance appearance) {
        spawn(client,p,dx,dy,dz,shape,size,primary,secondary,lifetime,gravity,fade,glow,scaling,easing,rotation,spin,active,false,appearance);
    }
    private void spawn(Minecraft client,Vec3 p,double dx,double dy,double dz,EffectParticle.Shape shape,float size,
            int primary,int secondary,int lifetime,float gravity,boolean fade,boolean glow,EffectParticle.Scaling scaling,
            Easing easing,float rotation,float spin,BooleanSupplier active,boolean ground,ParticleAppearance appearance) {
        ParticleTuning tuning=appearance==null?ParticleTuning.LEGACY:appearance.snapshot();
        if(!reserve(client,p,tuning.maxDistance()) || !active.getAsBoolean()) return;
        var random=client.level.random;
        double sample=tuning.randomness()==0?(sequence++%2==0?.25:.75):random.nextDouble();
        float radius=(float)Math.clamp(ParticleMath.variance(size,tuning.sizeVariance()*tuning.randomness(),sample),.005,2);
        int life=Math.clamp((int)Math.round(ParticleMath.variance(lifetime,tuning.lifetimeVariance()*tuning.randomness(),sample)),2,200);
        double velocity=ParticleMath.variance(tuning.velocityScale(),tuning.velocityVariance()*tuning.randomness(),sample);
        if(!ground) {
            dx*=velocity;dy*=velocity;dz*=velocity;
            if(tuning.scatter()>0 && tuning.randomness()>0) {
                double spread=tuning.scatter()*tuning.randomness();
                dx+=(random.nextDouble()*2-1)*spread;dy+=(random.nextDouble()*2-1)*spread;dz+=(random.nextDouble()*2-1)*spread;
            }
        }
        if(appearance!=null) shape=appearance.sprite(shape);
        client.particleEngine.add(new EffectParticle(client.level,p.x,p.y,p.z,dx,dy,dz,sprite(client,shape),radius,
                primary,secondary,life,ground?0:gravity+(float)tuning.gravityOffset(),fade,glow,scaling,easing,rotation,spin,
                active,ground,tuning,accent(),sample,client.level.getGameTime()/20.0,quality().liveCap,Math.min(tuning.maxDistance(),quality().distance)));
    }
    public void firefly(Minecraft client,Vec3 p,float size,int primary,int secondary,int lifetime,
            double speed,double drift,double phase,BooleanSupplier active,ParticleAppearance appearance,double twinkle,double twinkleSpeed) {
        var tuning=appearance.snapshot();
        if(!reserve(client,p,tuning.maxDistance()) || !active.getAsBoolean()) return;
        double sample=tuning.randomness()==0?.5:client.level.random.nextDouble();
        float radius=(float)ParticleMath.variance(size,tuning.sizeVariance()*tuning.randomness(),sample);
        int life=Math.clamp((int)Math.round(ParticleMath.variance(lifetime,tuning.lifetimeVariance()*tuning.randomness(),sample)),2,200);
        client.particleEngine.add(new FireflyParticle(client.level,p.x,p.y,p.z,sprite(client,appearance.sprite(EffectParticle.Shape.ORB)),radius,
                primary,secondary,life,speed,drift,phase,active,tuning,accent(),sample,client.level.getGameTime()/20.0,
                Math.min(tuning.maxDistance(),quality().distance),twinkle,twinkleSpeed));
    }
    public void ground(Minecraft client,Vec3 p,EffectParticle.Shape shape,float size,int primary,int secondary,
            int lifetime,EffectParticle.Scaling scaling,float rotation,BooleanSupplier active) {
        ground(client,p,shape,size,primary,secondary,lifetime,scaling,rotation,active,null);
    }
    public void ground(Minecraft client,Vec3 p,EffectParticle.Shape shape,float size,int primary,int secondary,
            int lifetime,EffectParticle.Scaling scaling,float rotation,BooleanSupplier active,ParticleAppearance appearance) {
        spawn(client,p,0,0,0,shape,size,primary,secondary,lifetime,0,true,true,scaling,Easing.OUT_CUBIC,rotation,0,active,true,appearance);
    }
    private boolean reserve(Minecraft client,Vec3 p,double distance) {
        if(client.level==null || client.options.particles().get()==ParticleStatus.MINIMAL) return false;
        double maximum=Math.min(distance,quality().distance);
        if(client.gameRenderer.getMainCamera().position().distanceToSqr(p)>=maximum*maximum) return false;
        return budget.admit(client.level,client.level.getGameTime(),quality(),client.options.particles().get()==ParticleStatus.DECREASED);
    }
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(Minecraft client,EffectParticle.Shape shape) {
        Identifier id=shape==EffectParticle.Shape.VANILLA?VANILLA_SPRITE:SPRITES[shape.ordinal()];
        return client.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(id);
    }
}
