package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

public final class FootstepEffectsModule extends VisualModule {
    public enum Style { PRINTS, PETALS, SPARKS, RIPPLES }
    private final EnumSetting<Style> style=add(new EnumSetting<>("style","Style","Alternating glowing sole marks, rising petals, short sparks or ground ripples.",Style.PRINTS,Style.class));
    private final ColorSetting primary=add(new ColorSetting("primary","Primary","ARGB with opacity.",0xBB88EAFF));
    private final ColorSetting secondary=add(new ColorSetting("secondary","Secondary","Fading color.",0x558FA5FF));
    private final DoubleSetting stride=add(new DoubleSetting("stride","Step spacing","Distance travelled between alternating visual footsteps.",.65,.25,1.5));
    private final DoubleSetting size=add(new DoubleSetting("size","Size","Particle radius in blocks.",0.16,.04,.35));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Ticks until a footprint or particle disappears.",28,6,70));
    private final BooleanSetting third=add(new BooleanSetting("third_person","Third person only","Disable emission in first person.",false));
    private final StepCadence cadence=new StepCadence();
    private final EffectEmitter emitter;
    private final BooleanSupplier active=this::enabled;
    private Vec3 previous;
    private Object level;
    private int foot;
    public final ParticleAppearance appearance;
    public FootstepEffectsModule(EffectEmitter emitter) {
        super("footstep_effects","Footstep Effects","Distance-paced left/right cosmetic marks at the local player's feet; stationary players emit nothing.",Category.PARTICLES);
        this.emitter=emitter;
        preset("Light Prints","Short glowing sole-shaped marks.");
        preset("Petal Walk","Soft pink petals rise from each footstep.","style","PETALS","primary","#CCD9A2E8","secondary","#778A9EFF","lifetime",34);
        preset("Ember Steps","Three tiny sparks per footstep.","style","SPARKS","primary","#FFFFCB7C","secondary","#66FF724D","lifetime",14);
        preset("Ripples","Small expanding surface rings.","style","RIPPLES","size",.2,"lifetime",20);
        preservePresetDefaults("size", 0.13);
        if(groups().isEmpty()) group("General",settings().toArray(Setting<?>[]::new));
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.FREE);
        appearance.movingWhen(()->style.get()==Style.PETALS||style.get()==Style.SPARKS);
        group("Particle colors",appearance.colors());
        group("Particle motion",appearance.motion());
        group("Particle advanced",appearance.advanced());
        preset("Soft Steps", "Refined size, motion and palette; fully editable.", "style", "PRINTS", "size", 0.16, "particle_envelope", true, "particle_fade_in", 0.08, "particle_fade_out", 0.5);
        preset("Crystal Steps", "Refined size, motion and palette; fully editable.", "style", "SPARKS", "size", 0.17, "particle_shape", "DIAMOND", "primary", "#EECEEBFF", "secondary", "#999CAAE8", "particle_size_variance", 0.1);

    }
    public void tick(Minecraft client) {
        if (!enabled() || client.level==null || client.player==null || client.isPaused() || !client.player.onGround()
                || client.player.isSpectator() || client.player.isInWater() || client.player.isInvisible()
                || (third.get()&&client.options.getCameraType().isFirstPerson())) { clear(); return; }
        Vec3 now=client.player.position();
        if (level==client.level && previous!=null) {
            double dx=now.x-previous.x,dz=now.z-previous.z;
            if(Math.abs(now.y-previous.y)>.7) cadence.reset();
            else {
                int count=cadence.advance(Math.sqrt(dx*dx+dz*dz),stride.get());
                for(int i=1;i<=count;i++) step(client,previous.lerp(now,(double)i/count));
            }
        }
        previous=now; level=client.level;
    }
    private void step(Minecraft client,Vec3 center) {
        double angle=Math.toRadians(client.player.getYRot()), side=(foot++%2==0?-1:1)*.13;
        Vec3 point=center.add(Math.cos(angle)*side,.035,Math.sin(angle)*side);
        float width=size.get().floatValue();
        switch(style.get()) {
            case PRINTS -> emitter.ground(client,point,EffectParticle.Shape.FOOTPRINT,width*1.5f,primary.get(),secondary.get(),lifetime.get(),EffectParticle.Scaling.CONSTANT,(float)-angle,active,appearance);
            case RIPPLES -> emitter.ground(client,point,EffectParticle.Shape.RING,width,primary.get(),secondary.get(),lifetime.get(),EffectParticle.Scaling.EXPAND,0,active,appearance);
            case PETALS, SPARKS -> {
                boolean spark=style.get()==Style.SPARKS;
                for(int i=0;i<(spark?3:2);i++) {
                    var random=client.level.random;
                    double randomness=appearance.randomness.get();
                    emitter.emit(client,point,(random.nextDouble()-.5)*.045*randomness,spark?.035:.018,(random.nextDouble()-.5)*.045*randomness,
                            spark?EffectParticle.Shape.SPARK:EffectParticle.Shape.SLASH,width*(spark?.55f:1),primary.get(),secondary.get(),lifetime.get(),spark?.18f:-.015f,
                            true,true,EffectParticle.Scaling.SHRINK,Easing.LINEAR,random.nextFloat()*(float)(6.28*randomness),.07f,active,appearance);
                }
            }
        }
    }
    private void clear() { previous=null;level=null;cadence.reset(); }
    @Override protected void onDisable() { clear(); }
}
