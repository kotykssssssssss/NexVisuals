package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.animation.EffectMath;
import dev.nexvisuals.core.visual.*;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Vanilla owns batching, frustum testing and disposal; this class adds bounded cosmetic tuning. */
public class EffectParticle extends SingleQuadParticle {
    public enum Shape { ORB, SPARK, RING, SLASH, STAR, FOOTPRINT, HEART, PIXEL, DOT, DIAMOND, STREAK, VANILLA }
    public enum Scaling { SHRINK, EXPAND, PULSE, CONSTANT }
    private static final Optional<ParticleLimit> LOW=Optional.of(new ParticleLimit(256)),MEDIUM=Optional.of(new ParticleLimit(512)),
            HIGH=Optional.of(new ParticleLimit(768)),ULTRA=Optional.of(new ParticleLimit(1024));
    protected final float startSize;
    protected final ParticleTuning tuning;
    protected final BooleanSupplier active;
    private final float spin;
    private final int primary,secondary,accent;
    private final double sample,seconds,maxDistance;
    private final Scaling scaling;
    private final boolean fade,luminous,groundPlane;
    private final Easing easing;
    private final Optional<ParticleLimit> limit;
    private static final FacingCameraMode GROUND=(rotation,camera,partial)->rotation.rotationX(-(float)Math.PI/2);

    public EffectParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,
            TextureAtlasSprite sprite,float size,int primary,int secondary,int lifetime,float gravity,boolean fade,boolean luminous,
            Scaling scaling,Easing easing,float rotation,float spin,BooleanSupplier active) {
        this(level,x,y,z,dx,dy,dz,sprite,size,primary,secondary,lifetime,gravity,fade,luminous,scaling,easing,rotation,spin,active,false);
    }
    public EffectParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,
            TextureAtlasSprite sprite,float size,int primary,int secondary,int lifetime,float gravity,boolean fade,boolean luminous,
            Scaling scaling,Easing easing,float rotation,float spin,BooleanSupplier active,boolean groundPlane) {
        this(level,x,y,z,dx,dy,dz,sprite,size,primary,secondary,lifetime,gravity,fade,luminous,scaling,easing,rotation,spin,active,groundPlane,
                ParticleTuning.LEGACY,0xFF8B9DFF,.5,0,768,64);
    }
    public EffectParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,
            TextureAtlasSprite sprite,float size,int primary,int secondary,int lifetime,float gravity,boolean fade,boolean luminous,
            Scaling scaling,Easing easing,float rotation,float spin,BooleanSupplier active,boolean groundPlane,ParticleTuning tuning,
            int accent,double sample,double seconds,int cap,double maxDistance) {
        super(level,x,y,z,sprite);
        this.groundPlane=groundPlane;this.tuning=tuning;this.startSize=size;this.quadSize=size;
        this.primary=primary;this.secondary=secondary;this.accent=accent;this.sample=sample;this.seconds=seconds;this.maxDistance=maxDistance;
        this.lifetime=Math.clamp(lifetime,2,200);this.gravity=gravity;this.fade=fade;this.luminous=luminous;this.scaling=scaling;
        this.easing=easing;this.roll=rotation;this.oRoll=rotation;this.spin=spin+(float)tuning.spin();this.active=active;
        this.hasPhysics=false;this.friction=(float)tuning.drag();setParticleSpeed(dx,dy,dz);
        limit=cap<=256?LOW:cap<=512?MEDIUM:cap<=768?HIGH:ULTRA;
        // Let vanilla frustum bounds include the expanded quad, not just a tiny centre.
        float bounds=(float)Math.min(6,size*(tuning.customScale()?Math.max(tuning.startSize(),tuning.endSize()):2)*2);
        setSize(Math.max(.2f,bounds),Math.max(.2f,bounds));setPos(x,y,z);
        appearance(0,1);
    }
    protected final void appearance(double progress,double legacyEnvelope) {
        int color=ParticleMath.color(tuning,primary,secondary,progress,sample,seconds+age/20.0,accent);
        setColor((color>>>16&255)/255f,(color>>>8&255)/255f,(color&255)/255f);
        double envelope=tuning.customEnvelope()?ParticleMath.envelope(progress,tuning.fadeIn(),tuning.fadeOut(),easing):legacyEnvelope;
        alpha=(color>>>24)/255f*(float)envelope;
    }
    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
    @Override public FacingCameraMode getFacingCameraMode() { return groundPlane?GROUND:super.getFacingCameraMode(); }
    @Override public Optional<ParticleLimit> getParticleLimit() { return limit; }
    @Override protected int getLightColor(float partial) {
        if(luminous && tuning.glow()>=1) return 0xF000F0;
        int vanilla=super.getLightColor(partial);
        return luminous?ParticleMath.light(vanilla,tuning.glow()):vanilla;
    }
    @Override public void extract(QuadParticleRenderState state,Camera camera,float partial) {
        double dx=x-camera.position().x,dy=y-camera.position().y,dz=z-camera.position().z;
        double visibility=ParticleMath.distanceFade(dx*dx+dy*dy+dz*dz,maxDistance,tuning.distanceFade());
        if(visibility<=0) return;
        float original=alpha;alpha*=(float)visibility;
        try { super.extract(state,camera,partial); } finally { alpha=original; }
    }
    @Override public float getQuadSize(float partial) {
        double p=EffectMath.unit((age+partial)/lifetime);
        double factor=tuning.customScale()?tuning.startSize()+(tuning.endSize()-tuning.startSize())*easing.apply(p):switch(scaling) {
            case EXPAND -> .2+1.8*easing.apply(p);
            case SHRINK -> 1-.9*p;
            case PULSE -> .4+.8*Math.sin(p*Math.PI);
            case CONSTANT -> 1;
        };
        return (float)Math.min(3,startSize*factor);
    }
    @Override public void tick() {
        if(!active.getAsBoolean()) { remove();return; }
        super.tick();oRoll=roll;roll+=spin;
        double p=EffectMath.unit((double)age/lifetime);
        appearance(p,fade?1-easing.apply(p):1);
    }
}
