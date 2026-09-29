package dev.nexvisuals.client.effect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.BooleanSetting;
import dev.nexvisuals.core.setting.DoubleSetting;
import net.minecraft.world.InteractionHand;

/** Transforms only the renderer's pushed hand matrix, never player/entity/gameplay state. */
public final class ViewmodelModule extends VisualModule {
    private final BooleanSetting separate = add(new BooleanSetting("separate_offhand", "Separate off hand", "Use independent transforms for the off hand.", false));
    private final HandSettings main = new HandSettings("main", "Main hand");
    private final HandSettings off = new HandSettings("off", "Off hand");
    public ViewmodelModule() {
        super("viewmodel", "Viewmodel", "Independent first-person hand position, rotation and per-axis scale.", Category.VIEWMODEL);
        preset("Vanilla", "Reset every transform to vanilla.");
        preset("Compact", "Smaller hands with a slight outward tilt.", "main_scale", .78, "main_y", -.12, "main_roll", -8);
        preset("Low", "Lower the item while keeping its size.", "main_y", -.3, "main_z", -.12, "main_pitch", 8);
        preset("Centered", "Bring the main hand toward the middle.", "main_x", -.32, "main_y", .08, "main_yaw", 18, "main_scale", .9);
        preset("PvP", "A compact, angled weapon silhouette.", "main_x", .12, "main_y", -.18, "main_pitch", -15, "main_roll", -22, "main_scale", .85);
        preset("Cinematic", "A larger side-on presentation.", "main_x", .2, "main_yaw", -28, "main_roll", 12, "main_scale", 1.12);
        action("Copy main to off hand", "Copy all main-hand transforms and enable independent off hand.", () -> copy(false));
        action("Mirror main to off hand", "Copy transforms, reversing X, yaw and roll.", () -> copy(true));
    }
    private void copy(boolean mirror) {
        for (var setting : settings()) {
            if (!setting.id().startsWith("main_")) continue;
            String target = "off_" + setting.id().substring(5);
            var value = setting.toJson();
            if (mirror && (target.equals("off_x") || target.equals("off_yaw") || target.equals("off_roll")))
                value = new com.google.gson.JsonPrimitive(-value.getAsDouble());
            for (var other : settings()) if (other.id().equals(target)) other.fromJson(value);
        }
        separate.set(true);
    }
    public void apply(PoseStack pose, InteractionHand hand) {
        if (!enabled()) return;
        HandSettings settings = hand == InteractionHand.OFF_HAND && separate.get() ? off : main;
        pose.translate(settings.x.get(), settings.y.get(), settings.z.get());
        pose.mulPose(Axis.XP.rotationDegrees(settings.pitch.get().floatValue()));
        pose.mulPose(Axis.YP.rotationDegrees(settings.yaw.get().floatValue()));
        pose.mulPose(Axis.ZP.rotationDegrees(settings.roll.get().floatValue()));
        float scale = settings.scale.get().floatValue();
        pose.scale(scale * settings.sx.get().floatValue(), scale * settings.sy.get().floatValue(), scale * settings.sz.get().floatValue());
    }
    private final class HandSettings {
        final DoubleSetting x, y, z, scale, pitch, yaw, roll, sx, sy, sz;
        HandSettings(String id, String label) {
            x = number(id + "_x", label + " X", 0, -1.5, 1.5);
            y = number(id + "_y", label + " Y", 0, -1.5, 1.5);
            z = number(id + "_z", label + " Z", 0, -1.5, 1.5);
            scale = number(id + "_scale", label + " scale", 1, .35, 2);
            pitch = number(id + "_pitch", label + " pitch", 0, -90, 90);
            yaw = number(id + "_yaw", label + " yaw", 0, -90, 90);
            roll = number(id + "_roll", label + " roll", 0, -90, 90);
            sx = number(id + "_scale_x", label + " scale X", 1, .25, 2);
            sy = number(id + "_scale_y", label + " scale Y", 1, .25, 2);
            sz = number(id + "_scale_z", label + " scale Z", 1, .25, 2);
        }
        private DoubleSetting number(String id, String name, double value, double min, double max) {
            return add(new DoubleSetting(id, name, "Visual transformation only. Reset restores the vanilla transform.", value, min, max));
        }
    }
}
