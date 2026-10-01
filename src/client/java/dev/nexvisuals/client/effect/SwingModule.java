package dev.nexvisuals.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import dev.nexvisuals.core.animation.SwingMotion.Style;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

/** Replaces only vanilla's ordinary first-person WHACK transform, not use/cooldown logic. */
public final class SwingModule extends VisualModule {
    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Swing style", "Bow charging, maps, eating and spear stabs retain vanilla animation.", Style.SMOOTH, Style.class));
    private final IntSetting duration = add(new IntSetting("duration", "Visual duration", "Milliseconds for the cosmetic motion only. Does not change attacks.", 300, 100, 900));
    private final DoubleSetting amplitude = add(new DoubleSetting("amplitude", "Amplitude", "Overall strength of the swing.", 1, 0, 2));
    private final DoubleSetting peak = add(new DoubleSetting("peak", "Attack / return balance", "Fraction of the animation spent reaching the peak; remaining time returns to neutral.", .35, .1, .9));
    private final EnumSetting<Easing> easing = add(new EnumSetting<>("easing", "Easing", "Parametric attack and return curve.", Easing.OUT_CUBIC, Easing.class));
    private final DoubleSetting x = number("x", "Custom translation X", -.35, -1.5, 1.5);
    private final DoubleSetting y = number("y", "Custom translation Y", .12, -1.5, 1.5);
    private final DoubleSetting z = number("z", "Custom translation Z", -.2, -1.5, 1.5);
    private final DoubleSetting pitch = number("pitch", "Custom pitch", -40, -180, 180);
    private final DoubleSetting yaw = number("yaw", "Custom yaw", 30, -180, 180);
    private final DoubleSetting roll = number("roll", "Custom roll", -60, -360, 360);
    private final DoubleSetting scale = number("scale", "Custom peak scale", 1, .5, 1.5);
    private final HandMotion right = new HandMotion(), left = new HandMotion();
    private Object playerIdentity, levelIdentity, mainItem, offItem;
    private HumanoidArm mainArm;
    private Style previousStyle;
    public SwingModule() {
        super("swing", "Item Swing", "Parametric first-person attack animation. No attack-speed or server changes.", Category.VIEWMODEL);
        preset("VANILLA", "Original Minecraft swing.", "style", "VANILLA");
        preset("SMOOTH", "A soft, readable downward swing.", "style", "SMOOTH");
        preset("SWIPE", "Quick sideways cut with a curved return.", "style", "SWIPE", "duration", 260, "peak", .28);
        preset("SLASH", "A strong diagonal cut, then a relaxed return.", "style", "SLASH", "duration", 320, "peak", .25);
        preset("SPIN", "One full visual rotation.", "style", "SPIN", "duration", 420, "easing", "IN_OUT_CUBIC");
        preset("PUSH", "Short forward thrust.", "style", "PUSH", "duration", 240, "peak", .3);
        preset("CUSTOM", "Tune the peak translation, rotation and scale.", "style", "CUSTOM");
        group("Animation", style, duration, amplitude, peak, easing);
        group("Custom pose", x, y, z, pitch, yaw, roll, scale);
    }
    private DoubleSetting number(String id, String label, double value, double min, double max) {
        return add(new DoubleSetting(id, label, "Peak transform in Custom mode. Starts and ends at your static viewmodel.", value, min, max));
    }
    /** Called only after vanilla accepted a local swing. Holding LMB does not create extra cycles. */
    public void started(LocalPlayer player, InteractionHand hand) {
        if (!enabled() || style.get() == Style.VANILLA) return;
        syncPlayer(player);
        start(hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(), player.tickCount, System.nanoTime());
    }
    void start(HumanoidArm arm, long tick, long now) {
        if (!enabled() || style.get() == Style.VANILLA) return;
        HandMotion motion = motion(arm);
        sample(motion, now);
        if (motion.timeline.start(tick, now)) motion.carry.copy(motion.pose);
    }
    private void syncPlayer(LocalPlayer player) {
        Object main = player.getMainHandItem().getItem(), off = player.getOffhandItem().getItem();
        if (playerIdentity != player || levelIdentity != player.level() || mainArm != player.getMainArm()) reset();
        if (mainItem != main) motion(player.getMainArm()).reset();
        if (offItem != off) motion(player.getMainArm().getOpposite()).reset();
        if (previousStyle != style.get()) { right.reset(); left.reset(); }
        playerIdentity = player; levelIdentity = player.level(); mainArm = player.getMainArm();
        mainItem = main; offItem = off; previousStyle = style.get();
    }
    public void tick(Minecraft client) {
        if (!enabled()) return;
        if (client.player == null || !client.options.getCameraType().isFirstPerson()
                || client.player.isUsingItem() || client.player.isAutoSpinAttack() || client.player.isSpectator()) { reset(); return; }
        syncPlayer(client.player);
    }
    public boolean apply(PoseStack pose, HumanoidArm arm) {
        if (!enabled() || style.get() == Style.VANILLA) return false;
        var player = Minecraft.getInstance().player;
        if (player == null || player.isUsingItem() || player.isAutoSpinAttack()) return false;
        return applyAt(pose, arm, System.nanoTime());
    }
    boolean applyAt(PoseStack pose, HumanoidArm arm, long now) {
        if (!enabled() || style.get() == Style.VANILLA) return false;
        HandMotion motion = motion(arm);
        sample(motion, now);
        if (!motion.timeline.active(now, duration.get()) || amplitude.get() == 0) return true;
        SwingMotion.Pose value = motion.pose;
        int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
        pose.translate(sign * value.x, value.y, value.z);
        pose.mulPose(Axis.XP.rotationDegrees((float) value.pitch)); pose.mulPose(Axis.YP.rotationDegrees((float) (sign * value.yaw)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (sign * value.roll)));
        float size = (float) Math.clamp(value.scale, .1, 3); pose.scale(size, size, size);
        return true;
    }
    private void sample(HandMotion motion, long now) {
        if (!motion.timeline.active(now, duration.get())) { motion.pose.identity(); return; }
        SwingMotion.sample(style.get(), motion.timeline.progress(now, duration.get()), peak.get(), easing.get(), amplitude.get(),
                x.get(), y.get(), z.get(), pitch.get(), yaw.get(), roll.get(), scale.get(), motion.pose);
        motion.pose.carry(motion.carry, SwingMotion.restartCarry(motion.timeline.elapsedNanos(now)));
    }
    private HandMotion motion(HumanoidArm arm) { return arm == HumanoidArm.RIGHT ? right : left; }
    private void reset() {
        right.reset(); left.reset(); playerIdentity = levelIdentity = mainItem = offItem = null; mainArm = null; previousStyle = null;
    }
    @Override protected void onDisable() { reset(); }
    public boolean moving(HumanoidArm arm) {
        return enabled() && style.get()!=Style.VANILLA && amplitude.get()>0
                && motion(arm).timeline.active(System.nanoTime(),duration.get());
    }
    private static final class HandMotion {
        final SwingTimeline timeline = new SwingTimeline();
        final SwingMotion.Pose pose = new SwingMotion.Pose(), carry = new SwingMotion.Pose();
        void reset() { timeline.reset(); pose.identity(); carry.identity(); }
    }
}
