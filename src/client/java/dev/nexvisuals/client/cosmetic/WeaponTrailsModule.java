package dev.nexvisuals.client.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.nexvisuals.client.effect.SwingModule;
import dev.nexvisuals.client.integration.RenderCompatibility;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.visual.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/** Samples the first rendered item layer after its display transform, scoped to the local held weapon. */
public final class WeaponTrailsModule extends VisualModule {
    public enum Alignment { AUTO, MODEL, LEGACY_HAND }
    private final ColorSetting primary=add(new ColorSetting("primary","Blade color","ARGB color of the newest sweep.",0xBB72DFFF));
    private final ColorSetting secondary=add(new ColorSetting("secondary","Tail color","ARGB color of the fading sweep.",0x448C7FFF));
    private final IntSetting lifetime=add(new IntSetting("lifetime","Lifetime","Milliseconds; at most 48 samples per hand, sampled at most 100 times per second.",150,40,350));
    private final BooleanSetting offhand=add(new BooleanSetting("offhand","Include off hand","Render the locally swinging offhand weapon too.",false));
    private final EnumSetting<Alignment> alignment=add(new EnumSetting<>("alignment","Blade alignment","Auto follows the item model, or retains edited legacy probes. Model uses the resource pack's actual display transform.",Alignment.AUTO,Alignment.class));
    private final DoubleSetting coverage=add(new DoubleSetting("coverage","Blade coverage","Length of the sweep along the model's upper diagonal. Smaller values create a narrower trail.",.65,.15,1));
    private final IntSetting smoothness=add(new IntSetting("smoothness","Smoothness","Bounded interpolation between samples; does not extend the trail.",2,1,3));
    private final DoubleSetting baseX=number("base_x","Blade base X",0),baseY=number("base_y","Blade base Y",-.15),baseZ=number("base_z","Blade base Z",0);
    private final DoubleSetting tipX=number("tip_x","Blade tip X",-.45),tipY=number("tip_y","Blade tip Y",.65),tipZ=number("tip_z","Blade tip Z",0);
    private final WeaponSweep main=new WeaponSweep(),off=new WeaponSweep();
    private final PoseStack identity=new PoseStack();
    private final Vector3f base=new Vector3f(),tip=new Vector3f();
    private Object level,mainItem,offItem;
    private WeaponSweep pending;
    private boolean modelSample,recording;
    private int side;
    private long now;
    private long settingRevision=Long.MIN_VALUE;
    private String status="";

    public WeaponTrailsModule() {
        super("weapon_trails","Weapon Trails","Short first-person sword/axe sweeps following your configured hand motion. Blade probes can be calibrated for item models.",Category.VIEWMODEL);
        preset("Prism","Soft cyan-to-violet blade sweep.");
        preset("Ember","Short warm sweep.","primary","#DDFFD080","secondary","#44FF684A","lifetime",110);
        preset("Frost","Longer pale blue sweep.","primary","#BBE8FAFF","secondary","#3379ABFF","lifetime",190);
        group("Appearance",primary,secondary,lifetime,coverage,smoothness,offhand);
        group("Blade alignment",alignment,baseX,baseY,baseZ,tipX,tipY,tipZ);
    }
    private DoubleSetting number(String id,String label,double value) {
        return add(new DoubleSetting(id,label,"Legacy hand-space probe. Auto uses these when edited; reset them to follow the model again. This is not a hitbox.",value,-1.5,1.5));
    }
    public void tick(Minecraft client) {
        if(!enabled() || client.level!=level || client.player==null || !client.options.getCameraType().isFirstPerson()) clear();
        if(enabled() && client.level!=null) level=client.level;
        if(!enabled()) return;
        long revision=0;for(var setting:settings()) revision+=setting.revision();
        if(revision!=settingRevision) { main.clear();off.clear();settingRevision=revision; }
    }
    public void started(LocalPlayer player,InteractionHand hand) {
        if(!enabled()) return;
        if(level!=player.level()) { clear();level=player.level(); }
        (hand==InteractionHand.MAIN_HAND?main:off).start(player.tickCount);
    }
    public boolean modelAlignment() {
        if(alignment.get()!=Alignment.AUTO) return alignment.get()==Alignment.MODEL;
        return baseX.get().equals(baseX.defaultValue()) && baseY.get().equals(baseY.defaultValue()) && baseZ.get().equals(baseZ.defaultValue())
                && tipX.get().equals(tipX.defaultValue()) && tipY.get().equals(tipY.defaultValue()) && tipZ.get().equals(tipZ.defaultValue());
    }
    /** Scope established around ItemInHandRenderer.renderItem; its original submit always runs. */
    public void begin(LivingEntity entity,ItemDisplayContext context,ItemStack stack,PoseStack pose,SwingModule animation) {
        pending=null;
        if(!enabled()) return;
        Minecraft client=Minecraft.getInstance();
        if(!(entity instanceof AbstractClientPlayer player)
                || (context!=ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && context!=ItemDisplayContext.FIRST_PERSON_LEFT_HAND)) return;
        HumanoidArm arm=context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND?HumanoidArm.RIGHT:HumanoidArm.LEFT;
        InteractionHand hand=arm==player.getMainArm()?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND;
        boolean isMain=hand==InteractionHand.MAIN_HAND;
        WeaponSweep sweep=isMain?main:off;
        if(player!=client.player || player.isSpectator() || player.isInvisible() || !client.options.getCameraType().isFirstPerson()
                || (!isMain && !offhand.get()) || player.isUsingItem() || player.isAutoSpinAttack()
                || (!stack.is(ItemTags.SWORDS) && !stack.is(ItemTags.AXES))) { sweep.clear(); return; }
        status=RenderCompatibility.blockReason();
        if(!status.isEmpty()) { main.clear();off.clear();return; }
        if(level!=client.level) { clear();level=client.level; }
        Object item=stack.getItem(),previousItem=isMain?mainItem:offItem;
        if(previousItem!=item) sweep.clear();
        if(isMain) mainItem=item;else offItem=item;
        now=System.nanoTime()/1_000_000;
        sweep.trim(now,lifetime.get());
        pending=sweep;side=arm==HumanoidArm.RIGHT?1:-1;modelSample=modelAlignment();
        // The same accepted swing drives both the motion and its trail. Native mode uses the local hand state.
        recording=animation.moving(arm) || (player.swinging && player.swingingArm==hand);
        if(!modelSample) sample(pose);
    }
    /** First layer only: layered/glint models cannot append multiple different probes in one frame. */
    public void sample(PoseStack pose) {
        if(pending==null || !recording) return;
        if(modelSample) {
            float start=(float)(.92-.72*coverage.get());
            base.set(start,start,.5f);tip.set(.92f,.92f,.5f);
        } else {
            base.set((float)(side*baseX.get()),baseY.get().floatValue(),baseZ.get().floatValue());
            tip.set((float)(side*tipX.get()),tipY.get().floatValue(),tipZ.get().floatValue());
        }
        pose.last().pose().transformPosition(base);pose.last().pose().transformPosition(tip);
        pending.sample(now,base.x,base.y,base.z,tip.x,tip.y,tip.z);
        recording=false;
    }
    public void end(SubmitNodeCollector collector) {
        WeaponSweep sweep=pending;pending=null;recording=false;
        if(sweep==null) return;
        RibbonMesh mesh=RibbonMesh.sweep(sweep.history(),now,lifetime.get(),primary.get(),secondary.get(),smoothness.get());
        if(mesh.vertices()==0) return;
        // Positions already contain this hand's transforms. The queue captures an identity pose and immutable mesh.
        collector.submitCustomGeometry(identity,RenderTypes.debugQuads(),(matrix,vertices)->{
            for(int i=0;i<mesh.vertices();i++) vertices.addVertex(matrix.pose(),mesh.coordinate(i,0),mesh.coordinate(i,1),mesh.coordinate(i,2)).setColor(mesh.color(i));
        });
    }
    @Override public String runtimeStatus() { return status; }
    private void clear() { main.clear();off.clear();pending=null;mainItem=offItem=level=null;recording=false;status=""; }
    @Override protected void onDisable() { clear(); }
}
