package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.visual.UseVisualTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.Vec3;
import java.util.function.BooleanSupplier;

/** Small particles around the local player's visible eat/drink animation; no entity or packet hooks. */
public final class ConsumptionEffectsModule extends VisualModule {
    public enum Style { PEARLS, PETALS, STEAM }
    private final EnumSetting<Style> style=add(new EnumSetting<>("style","Style","Soft pearls, drifting petals or a rising mist.",Style.PEARLS,Style.class));
    private final BooleanSetting eating=add(new BooleanSetting("eating","Eating","Decorate the local eat animation.",true));
    private final BooleanSetting drinking=add(new BooleanSetting("drinking","Drinking","Decorate the local drink animation.",true));
    private final BooleanSetting finish=add(new BooleanSetting("finish","Finish flourish","Only after observing the animation count down to its last tick. This does not confirm a server-side consume action.",true));
    private final IntSetting density=add(new IntSetting("density","Motes per pulse","One pulse every four local use ticks; maximum three motes per pulse.",1,1,3));
    private final IntSetting finishCount=add(new IntSetting("finish_count","Finish amount","Bounded small flourish after the observed use animation.",7,0,16));
    private final DoubleSetting size=add(new DoubleSetting("size","Particle size","World-space billboard radius in blocks.",.065,.025,.16));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Ticks; short effects near your own hands/head.",16,5,35));
    private final DoubleSetting speed=add(new DoubleSetting("speed","Drift speed","Only cosmetic particle motion.",.025,0,.1));
    private final DoubleSetting spread=add(new DoubleSetting("spread","Spawn spread","Small radius around the local mouth/held consumable.",.14,0,.35));
    private final ColorSetting primary=add(new ColorSetting("primary","Start color","ARGB including opacity.",0xCCE9E1B8));
    private final ColorSetting secondary=add(new ColorSetting("secondary","End color","ARGB at the end of life.",0x559ACBCB));
    public final ParticleAppearance appearance;
    private final EffectEmitter emitter;
    private final BooleanSupplier active=this::enabled;
    private final UseVisualTracker tracker=new UseVisualTracker();
    private long sequence;
    public ConsumptionEffectsModule(EffectEmitter emitter) {
        super("consumption_effects","Consumption FX","Local eating/drinking pearls, petals or mist, with a restrained finishing flourish. Does not change use timing or item state.",Category.PARTICLES);
        this.emitter=emitter;
        group("General",style,eating,drinking,finish,density,finishCount,size,lifetime,speed,spread);
        finishCount.visibleWhen(finish::get);
        appearance=new ParticleAppearance(this::add,ParticleAppearance.Kind.FREE);
        group("Colors",primary,secondary);group("Particle colors",appearance.colors());
        group("Motion",appearance.motion());group("Advanced",appearance.advanced());
        preset("Pearl Sip","Soft cream/cyan pearls and a short completion flourish.");
        preset("Tea Steam","Quiet pale rising mist; no finishing burst.","style","STEAM","primary","#99CFE8E5","secondary","#33B4CBDD","lifetime",26,"speed",.018,"finish",false,"particle_glow",.35,"particle_scale",true,"particle_start_size",.65,"particle_end_size",1.5);
        preset("Petal Feast","Slow lilac petals with a small diamond flourish.","style","PETALS","primary","#CCD6BCDE","secondary","#5598C7CF","particle_shape","DIAMOND","particle_rotation",22,"particle_size_variance",.12,"lifetime",22);
    }
    public void tick(Minecraft client) {
        if(!enabled() || client.level==null || client.player==null || client.isPaused() || client.player.isSpectator()) { tracker.reset();return; }
        var player=client.player;var stack=player.getUseItem();
        var animation=stack.getUseAnimation();
        boolean consuming=player.isUsingItem() && (animation==ItemUseAnimation.EAT && eating.get() || animation==ItemUseAnimation.DRINK && drinking.get());
        if(player.isUsingItem()&&!consuming) { tracker.reset();return; }
        var event=tracker.sample(client.level,consuming?stack.getItem():null,player.getUseItemRemainingTicks(),consuming);
        if(event==UseVisualTracker.Event.FINISHED && finish.get()) emit(client,finishCount.get(),true);
        else if(event==UseVisualTracker.Event.ACTIVE && player.getTicksUsingItem()%4==0) emit(client,density.get(),false);
    }
    private void emit(Minecraft client,int count,boolean flourish) {
        var player=client.player;Vec3 forward=player.getLookAngle();
        Vec3 origin=player.getEyePosition().add(forward.scale(.42)).add(0,-.24,0);
        double random=appearance.randomness.get();
        for(int i=0;i<count;i++) {
            double angle=(sequence++)*2.3999632297;
            double jitter=(client.level.random.nextDouble()-.5)*random;
            double radius=spread.get()*(.45+.25*jitter);
            Vec3 point=origin.add(Math.cos(angle)*radius,jitter*radius,Math.sin(angle)*radius);
            boolean mist=style.get()==Style.STEAM;
            var shape=style.get()==Style.PETALS?EffectParticle.Shape.DIAMOND:EffectParticle.Shape.ORB;
            double v=speed.get()*(flourish?1.5:1);
            emitter.emit(client,point,Math.cos(angle)*v,mist?v:Math.sin(angle)*v*.5,Math.sin(angle)*v,shape,size.get().floatValue(),primary.get(),secondary.get(),lifetime.get(),0,true,true,
                    mist?EffectParticle.Scaling.EXPAND:EffectParticle.Scaling.SHRINK,Easing.OUT_CUBIC,(float)angle,0,active,appearance);
        }
    }
    @Override protected void onDisable() { tracker.reset(); }
}
