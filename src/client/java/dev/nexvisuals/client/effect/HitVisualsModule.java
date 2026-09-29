package dev.nexvisuals.client.effect;

import dev.nexvisuals.client.module.HudModule;
import dev.nexvisuals.client.particle.CosmeticParticlesModule;
import dev.nexvisuals.client.particle.EffectEmitter;
import dev.nexvisuals.client.particle.EffectParticle;
import dev.nexvisuals.core.animation.Easing;
import net.minecraft.world.phys.Vec3;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Feedback for a local attack attempt. It does not assert that the server accepted damage. */
public final class HitVisualsModule extends VisualModule implements HudModule {
    public enum Style { BURST, SPARKS, RINGS, SLASH, IMPACT, CLASSIC }
    public enum ScaleMode { PRESET, SHRINK, EXPAND, PULSE, CONSTANT }
    private final EnumSetting<Style> style = add(new EnumSetting<>("preset", "Effect shape", "Distinct motion and geometry. Classic uses the optional Cosmetic Particles module.", Style.IMPACT, Style.class));
    private final BooleanSetting particles = add(new BooleanSetting("particles", "World effect", "Draw cosmetic feedback at the visible local attack point.", true));
    private final ColorSetting primary = add(new ColorSetting("primary", "Primary color", "ARGB including opacity.", 0xEE78D9FF));
    private final ColorSetting secondary = add(new ColorSetting("secondary", "Secondary color", "Color reached near the end of the effect.", 0xCCBA8CFF));
    private final DoubleSetting size = add(new DoubleSetting("size", "Effect size", "World-space radius multiplier.", 1, .2, 3));
    private final DoubleSetting intensity = add(new DoubleSetting("intensity", "Intensity", "Multiplies emission count, with a shared hard budget.", 1, .1, 2));
    private final DoubleSetting opacity = add(new DoubleSetting("opacity", "Effect opacity", "Multiplies the alpha of both world-effect colors.", 1, 0, 1));
    private final EnumSetting<ScaleMode> scaling = add(new EnumSetting<>("scale_animation", "Scale animation", "Keep the preset's motion or override how its shapes change size.", ScaleMode.PRESET, ScaleMode.class));
    private final IntSetting amount = add(new IntSetting("amount", "Detail amount", "Maximum detail particles per impact, not the number of full rings.", 24, 1, 64));
    private final IntSetting lifetime = add(new IntSetting("lifetime", "Lifetime", "Game ticks. 20 ticks is one second.", 16, 3, 60));
    private final DoubleSetting spread = add(new DoubleSetting("spread", "Spread", "Initial displacement from the visible attack position.", .12, 0, .8));
    private final DoubleSetting speed = add(new DoubleSetting("speed", "Speed", "Initial radial velocity in blocks per tick.", .16, 0, .5));
    private final DoubleSetting gravity = add(new DoubleSetting("gravity", "Gravity", "Cosmetic downward acceleration. Zero floats.", .3, -1, 2));
    private final BooleanSetting fade = add(new BooleanSetting("fade", "Fade", "Fade out over the particle lifetime.", true));
    private final BooleanSetting glow = add(new BooleanSetting("glow", "Luminous particles", "Full-bright particle colors with soft texture edges; no bloom or through-wall rendering.", true));
    private final BooleanSetting rotate = add(new BooleanSetting("random_rotation", "Random rotation", "Vary slash and spark rotation.", true));
    private final BooleanSetting accents = add(new BooleanSetting("accents", "Accent details", "Combine primary shapes with small motes or sparks.", true));
    private final EnumSetting<Easing> easing = add(new EnumSetting<>("easing", "Scale / fade curve", "Particle interpolation curve.", Easing.OUT_QUAD, Easing.class));
    private final BooleanSetting marker = add(new BooleanSetting("marker", "Attack marker", "Shows a marker for your local attack attempt, not a confirmed server hit.", true));
    private final IntSetting markerSize = add(new IntSetting("marker_size", "Marker size", "Length of each marker corner in GUI pixels.", 4, 2, 10));
    private final IntSetting duration = add(new IntSetting("duration", "Duration", "Marker and flash duration in milliseconds.", 180, 40, 600));
    private final ColorSetting color = add(new ColorSetting("color", "Feedback color", "ARGB color for the marker and subtle flash.", 0xCCB8D5FF));
    private final DoubleSetting flash = add(new DoubleSetting("flash", "Flash strength", "Additional cosmetic screen flash. Zero disables it.", 0, 0, .12));
    private long attackNanos;
    private Object attackLevel;
    private final CosmeticParticlesModule particleStyle;
    private final EffectEmitter emitter;
    private long lastEmission;
    public HitVisualsModule(CosmeticParticlesModule particleStyle, EffectEmitter emitter) {
        super("hit_visuals", "Hit Effects", "Expressive effects for a visible local attack attempt, not confirmed server damage.", Category.COMBAT);
        this.particleStyle = particleStyle;
        this.emitter = emitter;
        preset("Burst", "Round motes explode outward and shrink.", "preset", "BURST", "primary", "#EE67FFCE", "secondary", "#AA408FFF");
        preset("Sparks", "Thin fast golden sparks fall and shrink.", "preset", "SPARKS", "primary", "#FFFFDE80", "secondary", "#BBFF7448", "speed", .25, "gravity", .8, "lifetime", 12);
        preset("Rings", "Two expanding concentric rings with drifting accent motes.", "preset", "RINGS", "primary", "#EE9CA4FF", "secondary", "#BBEF92FF", "lifetime", 22);
        preset("Slash", "Two rotating tapered arcs cross the impact point.", "preset", "SLASH", "primary", "#EEF5F9FF", "secondary", "#AA7CE9FF", "lifetime", 12);
        preset("Impact", "Star flash, expanding shock ring and radial sparks.", "preset", "IMPACT", "flash", .035);
    }
    public void attacked(Minecraft client, Vec3 position) {
        if (!enabled() || client.level == null) return;
        long now = System.nanoTime();
        if (now - lastEmission < 50_000_000L) return;
        lastEmission = now; attackNanos = now; attackLevel = client.level;
        if (!particles.get()) return;
        if (style.get() == Style.CLASSIC) { particleStyle.burst(client, position, this::enabled); return; }
        float angle = rotate.get() ? client.level.random.nextFloat() * (float) (Math.PI * 2) : 0;
        switch (style.get()) {
            case RINGS -> {
                emit(client, position, EffectParticle.Shape.RING, .48f, 0, 0, 0, EffectParticle.Scaling.EXPAND, angle, 0);
                emit(client, position, EffectParticle.Shape.RING, .28f, 0, .012, 0, EffectParticle.Scaling.EXPAND, angle, 0);
            }
            case SLASH -> {
                emit(client, position, EffectParticle.Shape.SLASH, .75f, 0, 0, 0, EffectParticle.Scaling.EXPAND, angle, .08f);
                emit(client, position, EffectParticle.Shape.SLASH, .55f, 0, 0, 0, EffectParticle.Scaling.SHRINK, angle + 1.5f, -.05f);
            }
            case IMPACT -> {
                emit(client, position, EffectParticle.Shape.STAR, .65f, 0, 0, 0, EffectParticle.Scaling.SHRINK, angle, 0);
                emit(client, position, EffectParticle.Shape.RING, .5f, 0, 0, 0, EffectParticle.Scaling.EXPAND, 0, 0);
            }
            default -> { }
        }
        boolean core = style.get() == Style.BURST || style.get() == Style.SPARKS;
        if (!core && !accents.get()) return;
        int count = Math.min(96, Math.max(1, (int) (amount.get() * intensity.get() * (core ? 1 : .5))));
        for (int i = 0; i < count; i++) {
            var random = client.level.random;
            double azimuth = random.nextDouble() * Math.PI * 2, elevation = random.nextDouble() * 2 - 1;
            double radial = Math.sqrt(1 - elevation * elevation), dx = Math.cos(azimuth) * radial, dz = Math.sin(azimuth) * radial;
            Vec3 start = position.add(dx * spread.get(), elevation * spread.get(), dz * spread.get());
            boolean spark = style.get() == Style.SPARKS || style.get() == Style.IMPACT;
            emit(client, start, spark ? EffectParticle.Shape.SPARK : EffectParticle.Shape.ORB, spark ? .16f : .13f,
                    dx * speed.get(), elevation * speed.get(), dz * speed.get(), EffectParticle.Scaling.SHRINK,
                    rotate.get() ? random.nextFloat() * 6.283f : 0, spark ? .06f : 0);
        }
    }
    private void emit(Minecraft client, Vec3 p, EffectParticle.Shape shape, float baseSize, double dx, double dy, double dz,
                      EffectParticle.Scaling scale, float rotation, float spin) {
        boolean detail = shape == EffectParticle.Shape.ORB || shape == EffectParticle.Shape.SPARK;
        if (scaling.get() != ScaleMode.PRESET) scale = EffectParticle.Scaling.valueOf(scaling.get().name());
        emitter.emit(client, p, dx, dy, dz, shape, baseSize * size.get().floatValue(), Draw.withAlpha(primary.get(), opacity.get().floatValue()),
                Draw.withAlpha(secondary.get(), opacity.get().floatValue()), lifetime.get(),
                detail ? gravity.get().floatValue() : 0, fade.get(), glow.get(), scale, easing.get(), rotation, spin, this::enabled);
    }
    @Override public boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (client.player == null || client.level != attackLevel || attackNanos == 0 || client.screen != null) return false;
        double progress = (System.nanoTime() - attackNanos) / (duration.get() * 1_000_000.0);
        if (progress >= 1) return false;
        float opacity = (float) (1 - Math.max(0, progress));
        if (flash.get() > 0) Draw.rect(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), Draw.withAlpha(color.get(), opacity * flash.get().floatValue()));
        if (marker.get()) {
            int x = graphics.guiWidth() / 2, y = graphics.guiHeight() / 2;
            int argb = Draw.withAlpha(color.get(), opacity);
            for (int i = 0; i < markerSize.get(); i++) {
                int offset = 5 + i;
                Draw.rect(graphics, x - offset, y - offset, 1, 1, argb);
                Draw.rect(graphics, x + offset, y - offset, 1, 1, argb);
                Draw.rect(graphics, x - offset, y + offset, 1, 1, argb);
                Draw.rect(graphics, x + offset, y + offset, 1, 1, argb);
            }
        }
        return true;
    }
    @Override protected void onDisable() { attackNanos = 0; attackLevel = null; }
}
