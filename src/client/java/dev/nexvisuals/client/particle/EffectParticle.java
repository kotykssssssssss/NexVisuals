package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.animation.EffectMath;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** A textured vanilla billboard. Vanilla owns batching, depth testing and disposal. */
public final class EffectParticle extends SingleQuadParticle {
    public enum Shape { ORB, SPARK, RING, SLASH, STAR, FOOTPRINT, HEART, PIXEL }
    public enum Scaling { SHRINK, EXPAND, PULSE, CONSTANT }
    private static final Optional<ParticleLimit> LIMIT = Optional.of(new ParticleLimit(768));
    private final float startSize, spin;
    private final int primary, secondary;
    private final Scaling scaling;
    private final boolean fade, luminous;
    private final Easing easing;
    private final BooleanSupplier active;
    private final boolean groundPlane;
    private static final FacingCameraMode GROUND = (rotation, camera, partial) -> rotation.rotationX(-(float)Math.PI/2);

    public EffectParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                          TextureAtlasSprite sprite, float size, int primary, int secondary, int lifetime,
                          float gravity, boolean fade, boolean luminous, Scaling scaling, Easing easing,
                          float rotation, float spin, BooleanSupplier active) {
        this(level,x,y,z,dx,dy,dz,sprite,size,primary,secondary,lifetime,gravity,fade,luminous,scaling,easing,rotation,spin,active,false);
    }
    public EffectParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                          TextureAtlasSprite sprite, float size, int primary, int secondary, int lifetime,
                          float gravity, boolean fade, boolean luminous, Scaling scaling, Easing easing,
                          float rotation, float spin, BooleanSupplier active, boolean groundPlane) {
        super(level, x, y, z, sprite);
        this.groundPlane=groundPlane;
        this.startSize = size; this.quadSize = size; this.primary = primary; this.secondary = secondary;
        this.alpha = (primary >>> 24) / 255f; this.lifetime = lifetime;
        this.gravity = gravity; this.fade = fade; this.luminous = luminous; this.scaling = scaling;
        this.easing = easing; this.roll = rotation; this.oRoll = rotation; this.spin = spin; this.active = active;
        this.hasPhysics = false; this.friction = .94f; setParticleSpeed(dx, dy, dz); tint(primary);
    }
    private void tint(int color) { setColor((color >>> 16 & 255) / 255f, (color >>> 8 & 255) / 255f, (color & 255) / 255f); }
    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
    @Override public FacingCameraMode getFacingCameraMode() { return groundPlane ? GROUND : super.getFacingCameraMode(); }
    @Override public Optional<ParticleLimit> getParticleLimit() { return LIMIT; }
    @Override protected int getLightColor(float partialTick) { return luminous ? 0xF000F0 : super.getLightColor(partialTick); }
    @Override public float getQuadSize(float partialTick) {
        double p = EffectMath.unit((age + partialTick) / lifetime);
        return startSize * (float) switch (scaling) {
            case EXPAND -> .2 + 1.8 * easing.apply(p);
            case SHRINK -> 1 - .9 * p;
            case PULSE -> .4 + .8 * Math.sin(p * Math.PI);
            case CONSTANT -> 1;
        };
    }
    @Override public void tick() {
        if (!active.getAsBoolean()) { remove(); return; }
        super.tick(); oRoll = roll; roll += spin;
        double p = EffectMath.unit((double) age / lifetime);
        int tint = EffectMath.color(primary, secondary, p); tint(tint);
        alpha = (tint >>> 24) / 255f * (fade ? (float) (1 - easing.apply(p)) : 1);
    }
}
