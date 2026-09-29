package dev.nexvisuals.client.particle;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** Reusable style and bounded emitter; Minecraft handles simulation and render submission. */
public final class CosmeticParticlesModule extends VisualModule {
    private static final Identifier SPRITE = Identifier.withDefaultNamespace("generic_0");
    private final IntSetting amount = add(new IntSetting("amount", "Amount", "Particles per local attack burst. Total live particles are capped at 256.", 12, 1, 40));
    private final DoubleSetting size = add(new DoubleSetting("size", "Size", "Billboard half-size in world units.", .06, .01, .2));
    private final IntSetting lifetime = add(new IntSetting("lifetime", "Lifetime", "Lifetime in game ticks (20 ticks = one second).", 14, 2, 60));
    private final ColorSetting color = add(new ColorSetting("color", "Color", "ARGB particle tint and opacity.", 0xDDA5C8FF));
    private final DoubleSetting spread = add(new DoubleSetting("spread", "Velocity / spread", "Maximum initial velocity per axis.", .09, 0, .3));
    private final BooleanSetting fade = add(new BooleanSetting("fade", "Fade out", "Fade opacity over the particle's lifetime.", true));
    private final EnumSetting<Easing> easing = add(new EnumSetting<>("easing", "Fade easing", "Opacity interpolation curve.", Easing.OUT_QUAD, Easing.class));
    private long lastBurst;

    public CosmeticParticlesModule() { super("cosmetic_particles", "Classic Particle Style", "Legacy style used only by Hit Effects > CLASSIC. The five new styles have their own settings.", Category.PARTICLES); }

    public void burst(Minecraft client, Vec3 position, BooleanSupplier ownerEnabled) {
        if (!enabled() || client.level == null || client.options.particles().get() == ParticleStatus.MINIMAL) return;
        long now = System.nanoTime();
        if (now - lastBurst < 50_000_000L) return;
        lastBurst = now;
        int count = client.options.particles().get() == ParticleStatus.DECREASED ? Math.max(1, amount.get() / 2) : amount.get();
        var sprite = client.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(SPRITE);
        var random = client.level.random;
        double velocity = spread.get();
        BooleanSupplier active = () -> enabled() && ownerEnabled.getAsBoolean();
        for (int i = 0; i < count; i++) {
            client.particleEngine.add(new CosmeticSpark(client.level, position.x, position.y, position.z,
                    (random.nextDouble() * 2 - 1) * velocity, (random.nextDouble() * 2 - .5) * velocity,
                    (random.nextDouble() * 2 - 1) * velocity, sprite, size.get().floatValue(), color.get(),
                    lifetime.get(), fade.get(), easing.get(), active));
        }
    }
}
