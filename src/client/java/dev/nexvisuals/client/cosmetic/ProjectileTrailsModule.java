package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.EffectEmitter;
import dev.nexvisuals.client.particle.EffectParticle;
import dev.nexvisuals.client.particle.ParticleAppearance;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.visual.MotionTrail;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

/** Past positions of visible projectiles, through the shared depth-tested particle pipeline. */
public final class ProjectileTrailsModule extends VisualModule {
    public enum Style { COMET, SPARKS, HALOS }
    public static final int MAX_TRACKED = 32;
    public static final int MAX_PER_TICK = 48;
    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", "Soft comet stream, falling sparks or expanding halos.", Style.COMET, Style.class));
    private final BooleanSetting pearls = add(new BooleanSetting("pearls", "Ender pearls", "Decorate visible pearls; no prediction or destination markers.", true));
    private final BooleanSetting arrows = add(new BooleanSetting("arrows", "Arrows / tridents", "Decorate visible arrows and tridents.", true));
    private final BooleanSetting thrown = add(new BooleanSetting("thrown", "Other thrown items", "Include snowballs, eggs and thrown potions.", false));
    private final BooleanSetting own = add(new BooleanSetting("own_only", "Own projectiles only", "Requires the client to know the owner. Unknown owners are skipped, never guessed.", false));
    private final ColorSetting primary = add(new ColorSetting("primary", "Primary", "ARGB including opacity.", 0xD98AEAFF));
    private final ColorSetting secondary = add(new ColorSetting("secondary", "Secondary", "Fading tail color.", 0x70C59BFF));
    private final DoubleSetting size = add(new DoubleSetting("size", "Size", "Cosmetic particle radius.", 0.095, .025, .2));
    private final IntSetting density = add(new IntSetting("density", "Density", "Samples per block, at most eight per projectile and 48 total per tick.", 4, 1, 12));
    private final IntSetting lifetime = add(new IntSetting("lifetime", "Lifetime", "Particle lifetime in ticks.", 16, 4, 40));
    private final IntSetting distance = add(new IntSetting("distance", "Distance", "Maximum distance from your player; walls always interrupt emission.", 32, 8, 64));
    private final EffectEmitter emitter;
    private final BooleanSupplier active = this::enabled;
    private final Map<Integer, Tracked> tracked = new LinkedHashMap<>();
    private ClientLevel level;
    private boolean seeded;
    private record Tracked(Projectile entity, MotionTrail path) { }

    public final ParticleAppearance appearance;
    public ProjectileTrailsModule(EffectEmitter emitter) {
        super("projectile_trails", "Projectile Trails", "Comets, sparks and halos behind already visible projectiles. No trajectory prediction or hidden-entity tracking.", Category.PARTICLES);
        this.emitter = emitter;
        preset("Comet", "A fine cyan-violet stream along observed flight.");
        preset("Embers", "Short warm sparks drifting down.", "style", "SPARKS", "primary", "#FFFFCA7A", "secondary", "#80FF704C", "density", 3, "lifetime", 12);
        preset("Pearl Halos", "Spaced expanding circles behind pearls.", "style", "HALOS", "arrows", false, "density", 1, "size", .12, "lifetime", 20);
        group("Projectiles", pearls, arrows, thrown, own, distance);
        group("Appearance", style, primary, secondary, size, density, lifetime);
        preservePresetDefaults("size", 0.075);
        if(groups().isEmpty()) group("General",settings().toArray(Setting<?>[]::new));
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.FREE);
        group("Particle colors",appearance.colors());
        group("Particle motion",appearance.motion());
        group("Particle advanced",appearance.advanced());
        preset("Comet Plus", "Refined size, motion and palette; fully editable.", "size", 0.095, "density", 4, "particle_envelope", true, "particle_fade_in", 0.05, "particle_fade_out", 0.55);
        preset("Crystal Flight", "Refined size, motion and palette; fully editable.", "size", 0.09, "particle_shape", "DIAMOND", "particle_rotation", 45, "density", 3, "primary", "#EEBDEAFF", "secondary", "#888F9DD9");

    }

    private boolean supported(Entity entity) { return entity instanceof AbstractArrow || entity instanceof ThrowableItemProjectile; }
    private boolean selected(Projectile entity) {
        if (entity instanceof ThrownEnderpearl) return pearls.get();
        if (entity instanceof AbstractArrow) return arrows.get();
        return thrown.get();
    }
    public void loaded(Entity entity, ClientLevel world) {
        if (!enabled() || !supported(entity)) return;
        if (level != world) { clear(); level = world; }
        if (tracked.containsKey(entity.getId())) return;
        if (tracked.size() == MAX_TRACKED) tracked.remove(tracked.keySet().iterator().next());
        tracked.put(entity.getId(), new Tracked((Projectile) entity, new MotionTrail()));
    }
    public void unloaded(Entity entity, ClientLevel world) {
        if (level == world) tracked.remove(entity.getId());
    }

    public void tick(Minecraft client) {
        if (!enabled() || client.level == null || client.player == null) { clear(); return; }
        if (client.isPaused()) return;
        if (client.options.particles().get() == ParticleStatus.MINIMAL) { clear(); return; }
        if (level != client.level) { clear(); level = client.level; }
        if (!seeded) {
            // Enabling in an existing world needs one bootstrap, never a per-frame world search.
            int inspected = 0;
            for (Entity entity : level.entitiesForRendering()) {
                loaded(entity, level);
                if (++inspected >= 4096 || tracked.size() == MAX_TRACKED) break;
            }
            seeded = true;
        }
        int budget = MAX_PER_TICK;
        var iterator = tracked.values().iterator();
        while (iterator.hasNext()) {
            Tracked sample = iterator.next();
            Projectile entity = sample.entity();
            if (entity.isRemoved()) { iterator.remove(); continue; }
            if (!selected(entity) || entity.isInvisible() || entity.getDeltaMovement().lengthSqr() < .0004
                    || entity.distanceToSqr(client.player) > (double) distance.get() * distance.get()
                    || own.get() && entity.getOwner() != client.player || !client.player.hasLineOfSight(entity)) {
                sample.path().clear();
                continue;
            }
            int count = sample.path().observe(entity.getX(), entity.getY() + entity.getBbHeight() * .5, entity.getZ(), density.get(), budget);
            budget -= count;
            for (int i = 1; i <= count; i++) {
                double t = (double) i / count;
                emit(client, new Vec3(sample.path().x(t), sample.path().y(t), sample.path().z(t)));
            }
        }
    }

    private void emit(Minecraft client, Vec3 point) {
        boolean spark = style.get() == Style.SPARKS, halo = style.get() == Style.HALOS;
        emitter.emit(client, point, 0, spark ? .012 : 0, 0,
                spark ? EffectParticle.Shape.SPARK : halo ? EffectParticle.Shape.RING : EffectParticle.Shape.ORB,
                size.get().floatValue(), primary.get(), secondary.get(), lifetime.get(), spark ? .12f : 0,
                true, true, halo ? EffectParticle.Scaling.EXPAND : EffectParticle.Scaling.SHRINK, Easing.OUT_CUBIC,
                0, spark ? .1f : 0, active, appearance);
    }

    private void clear() { tracked.clear(); level = null; seeded = false; }
    @Override protected void onDisable() { clear(); }
}
