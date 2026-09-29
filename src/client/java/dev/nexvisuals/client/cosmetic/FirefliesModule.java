package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.EffectEmitter;
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
    private final DoubleSetting size=add(new DoubleSetting("size", "Size", "Small luminous billboard radius.", .045, .015, .12));
    private final DoubleSetting speed=add(new DoubleSetting("speed", "Drift speed", "Smooth drifting and hovering motion.", .7, .1, 2));
    private final DoubleSetting drift=add(new DoubleSetting("drift", "Drift distance", "Maximum orbit radius around each spawn.", .45, .05, 1.2));
    private final ColorSetting primary=add(new ColorSetting("primary", "Primary", "ARGB including opacity.", 0xDDEAFF8D));
    private final ColorSetting secondary=add(new ColorSetting("secondary", "Secondary", "Color toward the end of life.", 0xCC74D7AF));
    private final BooleanSetting nightOnly=add(new BooleanSetting("night_only", "Dusk / night only", "Use the actual world time; no changes to time or lighting.", true));
    private final BooleanSetting outdoors=add(new BooleanSetting("outdoors", "Open sky only", "Skip caves and covered spawn points.", true));
    private final EffectEmitter emitter;
    private final ParticleCadence cadence=new ParticleCadence();
    private Object level;

    public FirefliesModule(EffectEmitter emitter) {
        super("fireflies", "Fireflies", "Quiet drifting lights in the scenery, with depth-tested vanilla particles. No world lighting changes.", Category.PARTICLES);
        this.emitter=emitter;
        preset("Meadow", "Soft green lights outdoors at dusk.");
        preset("Embers", "Warm orange motes at any time.", "primary", "#EFFFBB65", "secondary", "#BBFF6938", "night_only", false, "speed", .4);
        preset("Moonlit", "Sparse blue and violet hovering lights.", "primary", "#DD9BDFFF", "secondary", "#CCCE9DFF", "amount", 24, "size", .035);
    }
    public void tick(Minecraft client) {
        if (!enabled() || client.level==null || client.player==null || client.isPaused() || client.player.isSpectator()) { clear(); return; }
        long time=Math.floorMod(client.level.getDayTime(), 24000);
        if (nightOnly.get() && (time<12000 || time>23500)) { cadence.reset(); return; }
        if (level!=client.level) { clear(); level=client.level; }
        int count=cadence.next(amount.get(), lifetime.get(), 3);
        var random=client.level.random;
        for (int i=0; i<count; i++) {
            double angle=random.nextDouble()*Math.PI*2;
            double distance=Math.sqrt(4+random.nextDouble()*(radius.get()*radius.get()-4));
            Vec3 point=client.player.position().add(Math.cos(angle)*distance, .3+random.nextDouble()*2, Math.sin(angle)*distance);
            BlockPos block=BlockPos.containing(point);
            if (client.level.getChunkSource().getChunk(block.getX() >> 4, block.getZ() >> 4, ChunkStatus.FULL, false)==null
                    || !client.level.getBlockState(block).isAir()
                    || (outdoors.get() && !client.level.canSeeSky(block))) continue;
            emitter.firefly(client, point, size.get().floatValue(), primary.get(), secondary.get(), lifetime.get(),
                    speed.get(), drift.get(), random.nextDouble()*Math.PI*2, this::enabled);
        }
    }
    private void clear() { cadence.reset(); level=null; }
    @Override protected void onDisable() { clear(); }
}
