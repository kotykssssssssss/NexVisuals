package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.ParticleCadence;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import java.util.function.BooleanSupplier;

/** Two bounded visible-surface probes per tick in actual local rain; no simulated weather or lighting. */
public final class RainRipplesModule extends VisualModule {
    public enum Surface { BOTH, GROUND, WATER }
    private final EnumSetting<Surface> surface=add(new EnumSetting<>("surface","Surfaces","Only loaded, exposed surfaces in actual rain; water uses the native fluid raycast.",Surface.BOTH,Surface.class));
    private final IntSetting density=add(new IntSetting("density","Surface probes / second","At most two attempts per tick. Hidden, sheltered, snowy and unloaded surfaces are skipped.",20,4,40));
    private final DoubleSetting radius=add(new DoubleSetting("radius","Spawn radius","Around your own position, with ordinary depth and a visibility raycast.",6,2,12));
    private final DoubleSetting size=add(new DoubleSetting("size","Ripple size","Expanding ring radius at spawn; actual final size uses the selected scale curve.",.12,.04,.3));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Ticks; no growing position history.",18,6,40));
    private final BooleanSetting splashes=add(new BooleanSetting("splashes","Water droplets","A small extra droplet above a visible wet surface.",true));
    private final ColorSetting primary=add(new ColorSetting("primary","Start color","Subtle blue-gray ripple ARGB.",0xAACBE2EC));
    private final ColorSetting secondary=add(new ColorSetting("secondary","End color","Fade towards the surface palette.",0x447FABB9));
    public final ParticleAppearance appearance;
    private final EffectEmitter emitter;
    private final ParticleCadence cadence=new ParticleCadence();
    private final BlockPos.MutableBlockPos probe=new BlockPos.MutableBlockPos();
    private final BooleanSupplier active=this::enabled;
    private Object level;
    private long sequence;
    public RainRipplesModule(EffectEmitter emitter) {
        super("rain_ripples","Rain Ripples","Short rings and droplets on visible exposed ground/water during real rain. Complements the screen Weather Lens; no weather changes.",Category.WORLD);
        this.emitter=emitter;appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.GROUND);
        group("General",surface,density,radius,size,lifetime,splashes);group("Colors",primary,secondary);
        group("Particle colors",appearance.colors());group("Motion",appearance.motion());group("Advanced",appearance.advanced());
        preset("Silver Rain","Readable but restrained silver-blue surface ripples.","particle_glow",.35);
        preset("Quiet Water","Wider, longer circles only on exposed water, without droplets.","surface","WATER","density",12,"size",.17,"lifetime",26,"splashes",false,"primary","#99BFDADF","particle_glow",.25);
        preset("Pearl Shower","Sharper, short rain impacts with gentle appearance fade.","size",.135,"density",28,"particle_envelope",true,"particle_fade_in",.08,"particle_fade_out",.65,"particle_size_variance",.15,"particle_glow",.5);
    }
    public void tick(Minecraft client) {
        if(!enabled() || client.level==null || client.player==null || client.isPaused() || !client.level.isRaining()
                || client.options.particles().get()==net.minecraft.server.level.ParticleStatus.MINIMAL) { reset();return; }
        if(level!=client.level) { reset();level=client.level; }
        int attempts=cadence.next((int)Math.round(density.get()*client.level.getRainLevel(1)),20,2);
        Vec3 eye=client.player.getEyePosition();
        for(int i=0;i<attempts;i++) {
            double random=appearance.randomness.get(),angle=(sequence++)*2.3999632297;
            double distance=radius.get()*(.25+.75*Math.sqrt((sequence*.61803398875)%1));
            angle+=(client.level.random.nextDouble()-.5)*random;
            double x=client.player.getX()+Math.cos(angle)*distance,z=client.player.getZ()+Math.sin(angle)*distance;
            int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
            probe.set(bx,0,bz);
            if(client.level.getChunkSource().getChunk(bx>>4,bz>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null) continue;
            int height=client.level.getHeight(Heightmap.Types.MOTION_BLOCKING,bx,bz);
            probe.set(bx,height,bz);
            if(!client.level.isRainingAt(probe) || Math.abs(height-client.player.getY())>6) continue;
            var hit=client.level.clip(new ClipContext(new Vec3(x,height+1,z),new Vec3(x,height-2,z),ClipContext.Block.COLLIDER,ClipContext.Fluid.SOURCE_ONLY,client.player));
            if(hit.getType()!=HitResult.Type.BLOCK || hit.getDirection()!=Direction.UP) continue;
            var fluid=client.level.getFluidState(hit.getBlockPos());boolean water=fluid.is(net.minecraft.tags.FluidTags.WATER);
            if(!fluid.isEmpty()&&!water) continue;
            if(surface.get()==Surface.WATER&&!water || surface.get()==Surface.GROUND&&water) continue;
            Vec3 point=hit.getLocation().add(0,.025,0);
            var visibility=client.level.clip(new ClipContext(eye,point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,client.player));
            if(visibility.getType()!=HitResult.Type.MISS) continue;
            emitter.ground(client,point,EffectParticle.Shape.RING,size.get().floatValue(),primary.get(),secondary.get(),lifetime.get(),EffectParticle.Scaling.EXPAND,0,active,appearance);
            if(splashes.get()) emitter.emit(client,point,0,.035,0,EffectParticle.Shape.DOT,size.get().floatValue()*.25f,primary.get(),secondary.get(),Math.min(12,lifetime.get()),.2f,true,true,
                    EffectParticle.Scaling.SHRINK,dev.nexvisuals.core.animation.Easing.OUT_CUBIC,0,0,active,appearance);
        }
    }
    private void reset() { cadence.reset();level=null; }
    @Override protected void onDisable() { reset(); }
}
