package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** Observes the vanilla placement context's cell; never changes prediction or packets. */
public final class BlockEffectsModule extends VisualModule {
    public enum Style { CRYSTAL, EMBERS, PETALS }
    private final EnumSetting<Style> style=add(new EnumSetting<>("style","Style","Sparkling shards, warm sparks or soft petal arcs.",Style.CRYSTAL,Style.class));
    private final BooleanSetting breaking=add(new BooleanSetting("breaking","Block break","After a successful local block break callback.",true));
    private final BooleanSetting placing=add(new BooleanSetting("placing","Block placement","Only a changed local placement cell after a successful BlockItem interaction. Server prediction can be corrected later.",true));
    private final ColorSetting primary=add(new ColorSetting("primary","Primary","ARGB with opacity.",0xDDD2FAFF));
    private final ColorSetting secondary=add(new ColorSetting("secondary","Secondary","End color.",0x779B8BFF));
    private final IntSetting amount=add(new IntSetting("amount","Amount","Particles per action, within the shared emission budget.",12,3,32));
    private final DoubleSetting size=add(new DoubleSetting("size","Size","Particle radius in blocks.",0.145,.025,.3));
    private final DoubleSetting speed=add(new DoubleSetting("speed","Spread speed","Cosmetic motion, not block-breaking speed.",.06,.015,.18));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Particle lifetime in ticks.",22,6,50));
    private final EffectEmitter emitter;
    private final BooleanSupplier active=this::enabled;
    private BlockPos candidate;
    private BlockState before;
    private Block expectedBlock;
    private Object observedLevel;
    public final ParticleAppearance appearance;
    public BlockEffectsModule(EffectEmitter emitter) {
        super("block_effects","Block Interaction FX","Extra particles for local breaking and observed predicted placement; keeps all vanilla block logic and effects.",Category.PARTICLES);
        this.emitter=emitter;
        preset("Crystal","Small cool shards.");
        preset("Workshop","Short outward golden sparks.","style","EMBERS","primary","#FFFFCD85","secondary","#88F67A46","lifetime",14);
        preset("Garden","Slow pink petals.","style","PETALS","primary","#DDD6A0EA","secondary","#7799D4FA","speed",.035,"lifetime",32);
        preservePresetDefaults("size", 0.12);
        if(groups().isEmpty()) group("General",settings().toArray(Setting<?>[]::new));
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.FREE);
        group("Particle colors",appearance.colors());
        group("Particle motion",appearance.motion());
        group("Particle advanced",appearance.advanced());
        preset("Crystal Plus", "Refined size, motion and palette; fully editable.", "size", 0.145, "amount", 14, "particle_size_variance", 0.12, "particle_envelope", true, "particle_fade_in", 0.05, "particle_fade_out", 0.5);
        preset("Workshop Flare", "Refined size, motion and palette; fully editable.", "style", "EMBERS", "size", 0.14, "amount", 16, "particle_drag", 0.97, "primary", "#FFF3D296", "secondary", "#99D5A088");

    }
    public void broken(Minecraft client,BlockPos pos) {
        if(enabled() && breaking.get() && client.player!=null && !client.player.isSpectator() && client.level!=null) burst(client,pos,false);
    }
    public void beforePlace(Minecraft client,LocalPlayer player,InteractionHand hand,BlockHitResult hit) {
        clearObservation();
        if(!enabled() || !placing.get() || client.level==null || player!=client.player || player.isSpectator()
                || !(player.getItemInHand(hand).getItem() instanceof BlockItem)) return;
        var stack=player.getItemInHand(hand);
        var context=new BlockPlaceContext(player,hand,stack,hit);
        if(!context.canPlace()) return;
        candidate=context.getClickedPos().immutable();observedLevel=client.level;
        before=client.level.getBlockState(candidate);expectedBlock=((BlockItem)stack.getItem()).getBlock();
    }
    public void afterPlace(Minecraft client,InteractionResult result) {
        if(candidate!=null && enabled() && placing.get() && client.level==observedLevel && result!=null && result.consumesAction()) {
            BlockState after=client.level.getBlockState(candidate);
            if(after!=before && after.is(expectedBlock)) burst(client,candidate,true);
        }
        clearObservation();
    }
    private void burst(Minecraft client,BlockPos pos,boolean place) {
        Vec3 center=Vec3.atCenterOf(pos);
        var random=client.level.random;
        for(int i=0;i<amount.get();i++) {
            double variation=appearance.randomness.get(),angle=i*2.39996323;
            double x=(random.nextDouble()-.5)*variation+Math.cos(angle)*.4*(1-variation);
            double y=(random.nextDouble()-.5)*variation+(.4-.8*(i+.5)/amount.get())*(1-variation);
            double z=(random.nextDouble()-.5)*variation+Math.sin(angle)*.4*(1-variation);
            if(place) {
                // Emit outside the new block rather than burying the decoration behind its depth.
                switch(i%3) {
                    case 0 -> x=Math.copySign(.54,x);
                    case 1 -> y=Math.copySign(.54,y);
                    case 2 -> z=Math.copySign(.54,z);
                }
            }
            boolean ember=style.get()==Style.EMBERS,petal=style.get()==Style.PETALS;
            emitter.emit(client,center.add(x,y,z),x*speed.get(),(place?.025:.015)+Math.abs(y)*speed.get(),z*speed.get(),
                    ember?EffectParticle.Shape.SPARK:petal?EffectParticle.Shape.SLASH:EffectParticle.Shape.STAR,
                    size.get().floatValue(),primary.get(),secondary.get(),lifetime.get(),petal?-.01f:.15f,true,true,
                    EffectParticle.Scaling.SHRINK,Easing.LINEAR,random.nextFloat()*(float)(6.28*variation),petal?.08f:.025f,active,appearance);
        }
    }
    private void clearObservation() { candidate=null;before=null;expectedBlock=null;observedLevel=null; }
    @Override protected void onDisable() { clearObservation(); }
}
