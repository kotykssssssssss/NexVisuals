package dev.nexvisuals.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public final class ShieldModule extends VisualModule {
    private final Transform resting = new Transform("idle", "Resting");
    private final BooleanSetting separate = add(new BooleanSetting("blocking", "Separate blocking pose", "Use the blocking transform while this hand is actively using the shield.", true));
    private final Transform blocking = new Transform("block", "Blocking");
    public ShieldModule() {
        super("shield", "Shield", "First-person shield position and size. Blocking mechanics remain vanilla.", Category.VIEWMODEL);
        preset("Vanilla", "Identity transforms, including while blocking.");
        preset("Compact", "Smaller shield, slightly lower while blocking.", "idle_scale", .72, "idle_y", -.14, "block_scale", .8, "block_y", -.15);
        preset("Minimal", "Small low shield silhouette.", "idle_scale", .5, "idle_y", -.28, "block_scale", .6, "block_y", -.22, "block_roll", -12);
    }
    public void apply(PoseStack pose, AbstractClientPlayer player, InteractionHand hand, ItemStack stack) {
        if (!enabled() || !(stack.getItem() instanceof ShieldItem)) return;
        Transform t = separate.get() && player.isUsingItem() && player.getUsedItemHand() == hand ? blocking : resting;
        pose.translate(t.x.get(), t.y.get(), t.z.get());
        pose.mulPose(Axis.XP.rotationDegrees(t.pitch.get().floatValue())); pose.mulPose(Axis.YP.rotationDegrees(t.yaw.get().floatValue()));
        pose.mulPose(Axis.ZP.rotationDegrees(t.roll.get().floatValue())); float s = t.scale.get().floatValue(); pose.scale(s, s, s);
    }
    private final class Transform {
        final DoubleSetting x, y, z, scale, pitch, yaw, roll;
        Transform(String id, String label) {
            x = number(id + "_x", label + " X", 0, -1.5, 1.5); y = number(id + "_y", label + " Y", 0, -1.5, 1.5);
            z = number(id + "_z", label + " Z", 0, -1.5, 1.5); scale = number(id + "_scale", label + " scale", 1, .25, 1.5);
            pitch = number(id + "_pitch", label + " pitch", 0, -90, 90); yaw = number(id + "_yaw", label + " yaw", 0, -90, 90);
            roll = number(id + "_roll", label + " roll", 0, -90, 90);
        }
        DoubleSetting number(String id, String label, double value, double min, double max) { return add(new DoubleSetting(id, label, "Additional shield-only transform after Viewmodel.", value, min, max)); }
    }
}
