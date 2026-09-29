package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** One shared per-tick emission budget for trails and impacts; no retained world history. */
public final class EffectEmitter {
    private static final Identifier[] SPRITES = java.util.Arrays.stream(EffectParticle.Shape.values())
            .map(s -> Identifier.fromNamespaceAndPath("nexvisuals", s.name().toLowerCase(java.util.Locale.ROOT))).toArray(Identifier[]::new);
    private Object level;
    private long tick = Long.MIN_VALUE;
    private int emitted;

    public void emit(Minecraft client, Vec3 position, double dx, double dy, double dz,
                     EffectParticle.Shape shape, float size, int primary, int secondary, int lifetime,
                     float gravity, boolean fade, boolean glow, EffectParticle.Scaling scaling, Easing easing,
                     float rotation, float spin, BooleanSupplier active) {
        if (!reserve(client)) return;
        var sprite = sprite(client, shape);
        client.particleEngine.add(new EffectParticle(client.level, position.x, position.y, position.z, dx, dy, dz,
                sprite, size, primary, secondary, lifetime, gravity, fade, glow, scaling, easing, rotation, spin, active));
    }

    public void firefly(Minecraft client, Vec3 position, float size, int primary, int secondary, int lifetime,
                        double speed, double drift, double phase, BooleanSupplier active) {
        if (!reserve(client)) return;
        client.particleEngine.add(new FireflyParticle(client.level, position.x, position.y, position.z,
                sprite(client, EffectParticle.Shape.ORB), size, primary, secondary, lifetime, speed, drift, phase, active));
    }

    private boolean reserve(Minecraft client) {
        if (client.level == null || client.options.particles().get() == ParticleStatus.MINIMAL) return false;
        long now = client.level.getGameTime();
        if (level != client.level || now != tick) { level = client.level; tick = now; emitted = 0; }
        int budget = client.options.particles().get() == ParticleStatus.DECREASED ? 48 : 128;
        return emitted++ < budget;
    }
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(Minecraft client, EffectParticle.Shape shape) {
        return client.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(SPRITES[shape.ordinal()]);
    }
}
