package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleLimit;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Uses vanilla's translucent, depth-tested particle pipeline and its built-in lifetime/budget. */
final class CosmeticSpark extends SingleQuadParticle {
    private static final Optional<ParticleLimit> LIMIT = Optional.of(new ParticleLimit(256));
    private final float initialAlpha;
    private final boolean fade;
    private final Easing easing;
    private final BooleanSupplier active;

    CosmeticSpark(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                  TextureAtlasSprite sprite, float size, int argb, int lifetime, boolean fade,
                  Easing easing, BooleanSupplier active) {
        super(level, x, y, z, sprite);
        this.quadSize = size;
        this.lifetime = lifetime;
        this.fade = fade;
        this.easing = easing;
        this.active = active;
        this.initialAlpha = (argb >>> 24) / 255f;
        this.alpha = initialAlpha;
        setColor(((argb >>> 16) & 255) / 255f, ((argb >>> 8) & 255) / 255f, (argb & 255) / 255f);
        setParticleSpeed(dx, dy, dz);
        this.hasPhysics = false;
        this.friction = .92f;
    }
    @Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
    @Override public Optional<ParticleLimit> getParticleLimit() { return LIMIT; }
    @Override public void tick() {
        if (!active.getAsBoolean()) { remove(); return; }
        super.tick();
        if (fade) alpha = initialAlpha * (float) (1 - easing.apply(Math.min(1, (double) age / lifetime)));
    }
}
