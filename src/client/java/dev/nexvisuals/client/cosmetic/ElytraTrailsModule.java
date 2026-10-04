package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Two symmetric trails sampled only from the local player's real gliding movement. */
public final class ElytraTrailsModule extends VisualModule {
    public enum Style { SILK, SPARKS, HALOS }
    private final EnumSetting<Style> style=add(new EnumSetting<>("style", "Style", "Soft twin streams, short sparks or expanding rings.", Style.SILK, Style.class));
    private final ColorSetting primary=add(new ColorSetting("primary", "Primary", "ARGB including opacity.", 0xD98AEAFF));
    private final ColorSetting secondary=add(new ColorSetting("secondary", "Secondary", "Fading tail color.", 0x88C59BFF));
    private final DoubleSetting span=add(new DoubleSetting("span", "Wing spacing", "Cosmetic distance between emitters; does not change the player model.", 1.4, .4, 3));
    private final DoubleSetting size=add(new DoubleSetting("size", "Width", "Billboard particle radius.", 0.125, .025, .3));
    private final IntSetting density=add(new IntSetting("density", "Quality / density", "Samples per block per wing, at most 16 particles per tick in total.", 4, 1, 12));
    private final IntSetting lifetime=add(new IntSetting("lifetime", "Lifetime", "Ticks until each sample disappears.", 22, 4, 60));
    private final BooleanSetting reactive=add(new BooleanSetting("speed_reactive", "Speed reactive", "Slightly widen the trail as real local flight speed increases.", true));
    private final BooleanSetting thirdPerson=add(new BooleanSetting("third_person", "Third person only", "Hide emission in first person.", true));
    private final EffectEmitter emitter;
    private Vec3 previousLeft, previousRight;
    private Object level;

    public final ParticleAppearance appearance;
    public ElytraTrailsModule(EffectEmitter emitter) {
        super("elytra_trails", "Elytra Trails", "Two local gliding trails with bounded particles and normal world depth. No flight changes.", Category.PARTICLES);
        this.emitter=emitter;
        preset("Aurora", "Soft twin cyan/violet wing trails.");
        preset("Comet", "Short warm sparks.", "style", "SPARKS", "primary", "#FFFFD490", "secondary", "#AAFF724C", "lifetime", 14, "density", 3);
        preset("Halo", "Paired expanding rings.", "style", "HALOS", "density", 1, "size", .14, "lifetime", 18);
        preservePresetDefaults("size", 0.1);
        if(groups().isEmpty()) group("General",settings().toArray(Setting<?>[]::new));
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.FREE);
        group("Particle colors",appearance.colors());
        group("Particle motion",appearance.motion());
        group("Particle advanced",appearance.advanced());
        preset("Silkstream", "Refined size, motion and palette; fully editable.", "size", 0.13, "density", 4, "particle_envelope", true, "particle_fade_in", 0.07, "particle_fade_out", 0.6);
        preset("Stardust Flight", "Refined size, motion and palette; fully editable.", "style", "SPARKS", "size", 0.12, "particle_shape", "STAR", "particle_size_variance", 0.15, "primary", "#EEE3D4FF", "secondary", "#8883CEEF");

    }
    public void tick(Minecraft client) {
        if (!enabled() || client.player==null || client.level==null || client.isPaused()
                || !client.player.isFallFlying() || client.player.isSpectator() || client.player.isInvisible()
                || (thirdPerson.get() && client.options.getCameraType().isFirstPerson())) { clear(); return; }
        Vec3 center=client.player.position().add(0, .35, 0);
        var left=FlightTrail.wing(center.x, center.y, center.z, client.player.getYRot(), span.get()/2, -1);
        var right=FlightTrail.wing(center.x, center.y, center.z, client.player.getYRot(), span.get()/2, 1);
        Vec3 a=new Vec3(left.x(),left.y(),left.z()), b=new Vec3(right.x(),right.y(),right.z());
        if (level==client.level && previousLeft!=null) {
            double distance=Math.max(a.distanceTo(previousLeft),b.distanceTo(previousRight));
            int count=FlightTrail.samples(distance,density.get());
            for (int i=1; i<=count; i++) {
                double t=(double)i/count;
                emit(client,previousLeft.lerp(a,t),distance);
                emit(client,previousRight.lerp(b,t),distance);
            }
        }
        previousLeft=a; previousRight=b; level=client.level;
    }
    private void emit(Minecraft client, Vec3 point, double speed) {
        boolean spark=style.get()==Style.SPARKS, halo=style.get()==Style.HALOS;
        float width=(float)(size.get()*(reactive.get() ? 1+Math.min(.5,speed*.2) : 1));
        emitter.emit(client,point,0,spark?.015:0,0, spark?EffectParticle.Shape.SPARK:halo?EffectParticle.Shape.RING:EffectParticle.Shape.ORB,
                width,primary.get(),secondary.get(),lifetime.get(),spark?.15f:0,true,true,
                halo?EffectParticle.Scaling.EXPAND:EffectParticle.Scaling.SHRINK,Easing.LINEAR,0,0,this::enabled,appearance);
    }
    private void clear() { previousLeft=null; previousRight=null; level=null; }
    @Override protected void onDisable() { clear(); }
}
