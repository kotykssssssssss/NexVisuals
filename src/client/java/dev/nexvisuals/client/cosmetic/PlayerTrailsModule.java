package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.awt.Color;

/** Tick sampling keeps no trail-point list; particle lifetime and the shared budget bound memory. */
public final class PlayerTrailsModule extends VisualModule {
    public enum Style { MOTES, SILK, SPARKS, RINGS, RAINBOW }
    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", "Silk uses closely overlapping fading billboards, not a custom ribbon renderer.", Style.SILK, Style.class));
    private final ColorSetting primary = add(new ColorSetting("primary", "Primary", "ARGB trail color.", 0xBB6EDFFF));
    private final ColorSetting secondary = add(new ColorSetting("secondary", "Secondary", "Color at the end of particle life.", 0x77CB8AFF));
    private final IntSetting density = add(new IntSetting("density", "Density / quality", "Samples per block travelled; maximum 12 emitted per tick.", 8, 1, 16));
    private final IntSetting lifetime = add(new IntSetting("lifetime", "Lifetime", "Particle life in ticks; no unbounded position history.", 18, 3, 60));
    private final DoubleSetting size = add(new DoubleSetting("size", "Width / size", "Particle radius in world units.", .13, .03, .4));
    private final DoubleSetting distance = add(new DoubleSetting("distance", "Minimum movement", "Emission begins after moving this distance in one tick.", .035, .01, .5));
    private final DoubleSetting height = add(new DoubleSetting("height", "Height above feet", "Local-player trail emitter height.", .15, .05, 1.7));
    private final BooleanSetting fade = add(new BooleanSetting("fade", "Fade", "Fade each trail sample.", true));
    private final BooleanSetting thirdPerson = add(new BooleanSetting("third_person", "Third person only", "Avoid covering the first-person view with your trail.", true));
    private final EffectEmitter emitter;
    private Object lastLevel;
    private Vec3 previous;
    public PlayerTrailsModule(EffectEmitter emitter) {
        super("player_trails", "Player Trails", "Bounded local-player particles, respecting ordinary world depth and particle settings.", Category.PARTICLES);
        this.emitter = emitter;
        preset("Silk", "Soft overlapping gradient motes.", "style", "SILK");
        preset("Ember", "Sparse golden sparks.", "style", "SPARKS", "primary", "#EEFFD080", "secondary", "#88FF684A", "density", 5);
        preset("Prism", "A cycling rainbow trail.", "style", "RAINBOW", "size", .16);
        preset("Ripples", "Expanding rings left along the path.", "style", "RINGS", "density", 3, "size", .2);
    }
    public void tick(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null || client.player.isSpectator() || client.player.isInvisible()
                || (thirdPerson.get() && client.options.getCameraType().isFirstPerson())) { previous = null; lastLevel = null; return; }
        Vec3 current = client.player.position();
        if (lastLevel != client.level || previous == null) { previous = current; lastLevel = client.level; return; }
        double travelled = current.distanceTo(previous);
        if (travelled >= distance.get() && travelled < 3) {
            int count = Math.min(12, Math.max(1, (int) Math.ceil(travelled * density.get())));
            for (int i = 0; i < count; i++) {
                Vec3 p = previous.lerp(current, (i + 1.0) / count).add(0, height.get(), 0);
                boolean spark = style.get() == Style.SPARKS, ring = style.get() == Style.RINGS;
                int color = style.get() == Style.RAINBOW ? (primary.get() & 0xFF000000) |
                        (Color.HSBtoRGB((client.level.getGameTime() % 160) / 160f, .65f, 1) & 0xFFFFFF) : primary.get();
                double jitter = style.get() == Style.MOTES || spark ? .02 : 0;
                emitter.emit(client, p, (client.level.random.nextDouble() - .5) * jitter, spark ? .035 : .002, 0,
                        spark ? EffectParticle.Shape.SPARK : ring ? EffectParticle.Shape.RING : EffectParticle.Shape.ORB,
                        size.get().floatValue(), color, secondary.get(), lifetime.get(), spark ? .25f : 0,
                        fade.get(), true, ring ? EffectParticle.Scaling.EXPAND : EffectParticle.Scaling.SHRINK,
                        Easing.LINEAR, spark ? client.level.random.nextFloat() * 6.283f : 0, 0, this::enabled);
            }
        }
        previous = current;
    }
    @Override protected void onDisable() { previous = null; lastLevel = null; }
}
