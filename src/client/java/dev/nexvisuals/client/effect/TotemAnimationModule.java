package dev.nexvisuals.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Alters only the pushed totem model pose, after native activation transforms. */
public final class TotemAnimationModule extends VisualModule {
    private final EnumSetting<TotemMotion.Style> style=add(new EnumSetting<>("style","Motion","Vanilla keeps native motion; other styles replace only the screen model's pose.",TotemMotion.Style.VANILLA,TotemMotion.Style.class));
    private final DoubleSetting size=number("size","Size",.65,.15,1.5);
    private final DoubleSetting x=number("x","Horizontal offset",0,-2,2),y=number("y","Vertical offset",0,-2,2);
    private final DoubleSetting depth=number("depth","Custom distance",2.8,1.5,5);
    private final DoubleSetting pitch=number("pitch","Pitch",0,-180,180),yaw=number("yaw","Yaw",0,-180,180),roll=number("roll","Roll",0,-180,180);
    private final DoubleSetting motion=number("motion","Motion strength",1,0,2),turns=number("turns","Spin turns",1,0,3);
    private final EnumSetting<Easing> easing=add(new EnumSetting<>("easing","Easing","Opening, closing and rotation curve. Activation duration remains vanilla.",Easing.OUT_CUBIC,Easing.class));
    public TotemAnimationModule() {
        super("totem_animation","Totem Animation","Customize the real local totem activation model. Keeps vanilla event, timer, particles and sounds.",Category.VIEWMODEL);
        preset("Classic","Native pose and full vanilla size.","size",1);
        preset("Compact","Native motion at a smaller scale.","size",.4,"x",.25,"y",-.2);
        preset("Float","Gentle floating model with smooth entrance and exit.","style","FLOAT","size",.8);
        preset("Spin","One complete turn with a smooth scale envelope.","style","SPIN","size",.85);
        preset("Pop","Quick expansion, slight roll and a soft return.","style","POP","size",.85,"easing","OUT_QUAD");
        group("Style & placement",style,size,x,y,depth);
        group("Motion",pitch,yaw,roll,motion,turns,easing);
    }
    private DoubleSetting number(String id,String label,double value,double min,double max) {
        return add(new DoubleSetting(id,label,"Cosmetic screen-model transform. Distance / motion / turns apply to custom styles.",value,min,max));
    }
    public void apply(PoseStack pose,ItemStack stack,int remaining,float partialTick) {
        if(!enabled() || stack==null || !stack.is(Items.TOTEM_OF_UNDYING)) return;
        if(style.get()!=TotemMotion.Style.VANILLA) {
            var frame=TotemMotion.sample(style.get(),(40-remaining+partialTick)/40.0,easing.get(),motion.get(),turns.get());
            // ScreenEffectRenderer supplies an identity root and has already pushed this activation model.
            pose.setIdentity();
            pose.translate(x.get(),y.get()+frame.y(),-depth.get()+frame.z());
            pose.mulPose(Axis.XP.rotationDegrees((float)(pitch.get()+frame.pitch())));
            pose.mulPose(Axis.YP.rotationDegrees((float)(yaw.get()+frame.yaw())));
            pose.mulPose(Axis.ZP.rotationDegrees((float)(roll.get()+frame.roll())));
            float scale=(float)Math.max(.0001,size.get()*frame.scale());pose.scale(scale,scale,scale);
        } else {
            pose.last().pose().translateLocal(x.get().floatValue(),y.get().floatValue(),0);
            pose.mulPose(Axis.XP.rotationDegrees(pitch.get().floatValue()));
            pose.mulPose(Axis.YP.rotationDegrees(yaw.get().floatValue()));
            pose.mulPose(Axis.ZP.rotationDegrees(roll.get().floatValue()));
            float scale=size.get().floatValue();pose.scale(scale,scale,scale);
        }
    }
}
