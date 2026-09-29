package dev.nexvisuals.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.world.entity.HumanoidArm;

/** Replaces only vanilla's ordinary first-person WHACK transform, not use/cooldown logic. */
public final class SwingModule extends VisualModule {
    public enum Style { VANILLA, SMOOTH, SWIPE, SLASH, SPIN, PUSH, CUSTOM }
    private final EnumSetting<Style> style = add(new EnumSetting<>("style", "Swing style", "Bow charging, maps, eating and spear stabs retain vanilla animation.", Style.SMOOTH, Style.class));
    private final IntSetting duration = add(new IntSetting("duration", "Visual duration", "Milliseconds for the cosmetic motion only. Does not change attacks.", 300, 100, 900));
    private final DoubleSetting amplitude = add(new DoubleSetting("amplitude", "Amplitude", "Overall strength of the swing.", 1, 0, 2));
    private final DoubleSetting peak = add(new DoubleSetting("peak", "Attack / return balance", "Fraction of the animation spent reaching the peak; remaining time returns to neutral.", .35, .1, .9));
    private final EnumSetting<Easing> easing = add(new EnumSetting<>("easing", "Easing", "Parametric attack and return curve.", Easing.IN_OUT_CUBIC, Easing.class));
    private final DoubleSetting x = number("x", "Custom translation X", -.35, -1.5, 1.5);
    private final DoubleSetting y = number("y", "Custom translation Y", .12, -1.5, 1.5);
    private final DoubleSetting z = number("z", "Custom translation Z", -.2, -1.5, 1.5);
    private final DoubleSetting pitch = number("pitch", "Custom pitch", -40, -180, 180);
    private final DoubleSetting yaw = number("yaw", "Custom yaw", 30, -180, 180);
    private final DoubleSetting roll = number("roll", "Custom roll", -60, -360, 360);
    private final DoubleSetting scale = number("scale", "Custom peak scale", 1, .5, 1.5);
    private final SwingTimeline right = new SwingTimeline(), left = new SwingTimeline();
    public SwingModule() {
        super("swing", "Item Swing", "Parametric first-person attack animation. No attack-speed or server changes.", Category.VIEWMODEL);
        for (Style value : Style.values()) preset(value.name(), "Apply this animation style with balanced defaults.", "style", value.name());
    }
    private DoubleSetting number(String id, String label, double value, double min, double max) {
        return add(new DoubleSetting(id, label, "Peak transform in Custom mode. Starts and ends at your static viewmodel.", value, min, max));
    }
    public boolean apply(PoseStack pose, HumanoidArm arm, float vanillaProgress) {
        if (!enabled() || style.get() == Style.VANILLA) return false;
        double p = (arm == HumanoidArm.RIGHT ? right : left).sample(vanillaProgress, System.nanoTime(), duration.get());
        double a = EffectMath.envelope(p, peak.get(), easing.get()) * amplitude.get();
        int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
        double tx = 0, ty = 0, tz = 0, rx = 0, ry = 0, rz = 0, s = 1;
        switch (style.get()) {
            case SMOOTH -> { ty = .1 * a; tz = -.12 * a; rx = -32 * a; ry = 16 * a; rz = -24 * a; }
            case SWIPE -> { tx = -.48 * a; ty = .04 * a; ry = 55 * a; rz = -62 * a; }
            case SLASH -> { tx = -.3 * a; ty = -.22 * a; rx = -70 * a; rz = -95 * a; }
            case SPIN -> { ty = .15 * a; rz = amplitude.get() == 0 ? 0 : 360 * easing.get().apply(p); }
            case PUSH -> { tz = -.55 * a; rx = -12 * a; s = 1 + .08 * a; }
            case CUSTOM -> { tx = x.get() * a; ty = y.get() * a; tz = z.get() * a; rx = pitch.get() * a; ry = yaw.get() * a; rz = roll.get() * a; s = 1 + (scale.get() - 1) * a; }
            default -> { }
        }
        pose.translate(sign * tx, ty, tz);
        pose.mulPose(Axis.XP.rotationDegrees((float) rx)); pose.mulPose(Axis.YP.rotationDegrees((float) (sign * ry)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (sign * rz))); s = Math.clamp(s, .1, 3); pose.scale((float) s, (float) s, (float) s);
        return true;
    }
    @Override protected void onDisable() { right.reset(); left.reset(); }
}
