package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.visual.ParticleTuning;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;

/** Analytic hovering retained; color, shapes, distance and envelopes reuse the common particle. */
public final class FireflyParticle extends EffectParticle {
    private static final Optional<ParticleLimit> LIMIT=Optional.of(new ParticleLimit(96));
    private final double originX,originY,originZ,phase,speed,drift,twinkle,twinkleSpeed;
    public FireflyParticle(ClientLevel level,double x,double y,double z,TextureAtlasSprite sprite,float size,
            int primary,int secondary,int lifetime,double speed,double drift,double phase,BooleanSupplier active,
            ParticleTuning tuning,int accent,double sample,double seconds,double maxDistance,double twinkle,double twinkleSpeed) {
        super(level,x,y,z,0,0,0,sprite,size,primary,secondary,lifetime,0,true,true,Scaling.CONSTANT,Easing.LINEAR,
                0,0,active,false,tuning,accent,sample,seconds,96,maxDistance);
        originX=x;originY=y;originZ=z;this.speed=speed;this.drift=drift;this.phase=phase;alpha=0;
        this.twinkle=twinkle;this.twinkleSpeed=twinkleSpeed;
    }
    @Override public Optional<ParticleLimit> getParticleLimit() { return LIMIT; }
    @Override public float getQuadSize(float partial) {
        return tuning.customScale()?super.getQuadSize(partial):startSize*(float)(.75+.25*Math.sin(phase+(age+partial)*.15));
    }
    @Override public void tick() {
        if(!active.getAsBoolean()) { remove();return; }
        xo=x;yo=y;zo=z;
        if(++age>=lifetime) { remove();return; }
        double t=age*.045*speed;
        setPos(originX+drift*(Math.sin(phase+t)-Math.sin(phase)),originY+drift*.35*(Math.sin(phase+t*1.7)-Math.sin(phase)),
                originZ+drift*(Math.cos(phase+t*.8)-Math.cos(phase)));
        oRoll=roll;roll+=(float)tuning.spin();
        double edge=Math.min(1,Math.min(age/10.0,(lifetime-age)/15.0));
        appearance((double)age/lifetime,edge);
        alpha*=(float)(1-twinkle+twinkle*Math.pow(Math.sin(phase+age*twinkleSpeed),2));
    }
}
