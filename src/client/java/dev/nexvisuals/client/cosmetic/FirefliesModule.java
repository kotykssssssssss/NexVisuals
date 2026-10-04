package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.EffectEmitter;
import dev.nexvisuals.client.particle.ParticleAppearance;
import dev.nexvisuals.core.animation.ParticleCadence;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/** Ambient scenery around the local player; no entities, tracking or simulated gameplay lights. */
public final class FirefliesModule extends VisualModule {
    private final IntSetting amount=add(new IntSetting("amount", "Population", "Target population; hard cap 96 and at most 3 spawns per tick.", 40, 4, 80));
    private final IntSetting lifetime=add(new IntSetting("lifetime", "Lifetime", "Ticks; existing motes fade out naturally.", 100, 30, 160));
    private final DoubleSetting radius=add(new DoubleSetting("radius", "Radius", "Scatter within this distance, outside the immediate first-person view.", 7, 3, 14));
    private final DoubleSetting size=add(new DoubleSetting("size", "Size", "Small luminous billboard radius.", 0.056, .015, .12));
    private final DoubleSetting speed=add(new DoubleSetting("speed", "Drift speed", "Smooth drifting and hovering motion.", .7, .1, 2));
    private final DoubleSetting drift=add(new DoubleSetting("drift", "Drift distance", "Maximum orbit radius around each spawn.", .45, .05, 1.2));
    private final ColorSetting primary=add(new ColorSetting("primary", "Primary", "ARGB including opacity.", 0xDDEAFF8D));
    private final ColorSetting secondary=add(new ColorSetting("secondary", "Secondary", "Color toward the end of life.", 0xCC74D7AF));
    private final BooleanSetting nightOnly=add(new BooleanSetting("night_only", "Dusk / night only", "Use the actual world time; no changes to time or lighting.", true));
    private final BooleanSetting outdoors=add(new BooleanSetting("outdoors", "Open sky only", "Skip caves and covered spawn points.", true));
    private final EffectEmitter emitter;
    private final ParticleCadence cadence=new ParticleCadence();
    private Object level;

    public final ParticleAppearance appearance;
    private final DoubleSetting twinkle=add(new DoubleSetting("twinkle","Twinkle strength","Zero keeps steady light; no simulated gameplay illumination.",.45,0,1));
    private final DoubleSetting twinkleSpeed=add(new DoubleSetting("twinkle_speed","Twinkle speed","Smooth brightness oscillation in radians per tick.",.11,0,.3));
    private long sequence;
    public FirefliesModule(EffectEmitter emitter) {
        super("fireflies", "Fireflies", "Quiet drifting lights in the scenery, with depth-tested vanilla particles. No world lighting changes.", Category.PARTICLES);
        this.emitter=emitter;
        preset("Meadow", "Soft green lights outdoors at dusk.");
        preset("Embers", "Warm orange motes at any time.", "primary", "#EFFFBB65", "secondary", "#BBFF6938", "night_only", false, "speed", .4);
        preset("Moonlit", "Sparse blue and violet hovering lights.", "primary", "#DD9BDFFF", "secondary", "#CCCE9DFF", "amount", 24, "size", .035);
        preservePresetDefaults("size", 0.045);
        if(groups().isEmpty()) group("General",settings().toArray(Setting<?>[]::new));
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.AMBIENT);
        group("Particle colors",appearance.colors());
        group("Particle motion",appearance.motion());
        group("Particle advanced",appearance.advanced());
        preset("Lantern Meadow", "Refined size, motion and palette; fully editable.", "size", 0.06, "amount", 32, "particle_size_variance", 0.12, "particle_opacity", 0.95);
        preset("Starlit Garden", "Refined size, motion and palette; fully editable.", "size", 0.055, "particle_shape", "STAR", "primary", "#E8BDD9FF", "secondary", "#BBC5B3EF", "amount", 28, "particle_rotation", 12);

    }
    public void tick(Minecraft client) {
        if (!enabled() || client.level==null || client.player==null || client.isPaused() || client.player.isSpectator()) { clear(); return; }
        if (level!=client.level) { clear(); level=client.level; }
        long time=Math.floorMod(client.level.getDayTime(), 24000);
        if (nightOnly.get() && (time<12000 || time>23500)) { cadence.reset(); return; }
        int count=cadence.next(amount.get(), lifetime.get(), 3);
        var random=client.level.random;
        for (int i=0; i<count; i++) {
            double variation=appearance.randomness.get(),index=sequence++;
            double angle=random.nextDouble()*Math.PI*2*variation+index*2.39996323*(1-variation);
            double sample=random.nextDouble()*variation+((index*.61803398875)%1)*(1-variation);
            double distance=Math.sqrt(4+sample*(radius.get()*radius.get()-4));
            Vec3 point=client.player.position().add(Math.cos(angle)*distance, .3+(random.nextDouble()*variation+.5*(1-variation))*2, Math.sin(angle)*distance);
            BlockPos block=BlockPos.containing(point);
            if (client.level.getChunkSource().getChunk(block.getX() >> 4, block.getZ() >> 4, ChunkStatus.FULL, false)==null
                    || !client.level.getBlockState(block).isAir()
                    || (outdoors.get() && !client.level.canSeeSky(block))) continue;
            emitter.firefly(client, point, size.get().floatValue(), primary.get(), secondary.get(), lifetime.get(),
                    speed.get(), drift.get(), random.nextDouble()*Math.PI*2*variation+angle*(1-variation), this::enabled, appearance,twinkle.get(),twinkleSpeed.get());
        }
    }
    private void clear() { cadence.reset(); level=null; }
    @Override protected void onDisable() { clear(); }
}
