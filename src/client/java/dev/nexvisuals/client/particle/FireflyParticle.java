package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.EffectMath;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;

/** Small ambient motes with analytic drift and twinkle; vanilla owns depth testing and disposal. */
public final class FireflyParticle extends SingleQuadParticle {
    private static final Optional<ParticleLimit> LIMIT = Optional.of(new ParticleLimit(96));
    private final double originX, originY, originZ, phase, speed, drift;
    private final float size;
    private final int primary, secondary;
    private final BooleanSupplier active;

    public FireflyParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite,
                           float size, int primary, int secondary, int lifetime, double speed,
                           double drift, double phase, BooleanSupplier active) {
        super(level, x, y, z, sprite);
        originX=x; originY=y; originZ=z; this.size=size; this.primary=primary; this.secondary=secondary;
        this.speed=speed; this.drift=drift; this.phase=phase; this.active=active;
        this.lifetime=lifetime; this.hasPhysics=false; this.quadSize=size; this.alpha=0;
    }
    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
    @Override public Optional<ParticleLimit> getParticleLimit() { return LIMIT; }
    @Override protected int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public float getQuadSize(float partialTick) {
        return size * (float) (.75 + .25 * Math.sin(phase + (age+partialTick)*.15));
    }
    @Override public void tick() {
        if (!active.getAsBoolean()) { remove(); return; }
        xo=x; yo=y; zo=z;
        if (++age >= lifetime) { remove(); return; }
        double t=age*.045*speed;
        setPos(originX + drift*(Math.sin(phase+t)-Math.sin(phase)),
                originY + drift*.35*(Math.sin(phase+t*1.7)-Math.sin(phase)),
                originZ + drift*(Math.cos(phase+t*.8)-Math.cos(phase)));
        int color=EffectMath.color(primary, secondary, (double)age/lifetime);
        setColor((color>>>16&255)/255f, (color>>>8&255)/255f, (color&255)/255f);
        double edge=Math.min(1, Math.min(age/10.0, (lifetime-age)/15.0));
        alpha=(color>>>24)/255f * (float)(edge*(.55+.45*Math.pow(Math.sin(phase+age*.11),2)));
    }
}
