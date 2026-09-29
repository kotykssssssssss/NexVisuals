package dev.nexvisuals.client.cosmetic;

import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

/** A player feature layer is culled/submitted with the ordinary entity, not an ESP overlay. */
public final class CosmeticHatModule extends VisualModule {
    final DoubleSetting radius = number("radius", "Radius", .55, .25, 1);
    final DoubleSetting height = number("height", "Cone height", .28, .08, .65);
    final DoubleSetting offset = number("offset", "Vertical offset", .04, -.05, .4);
    final ColorSetting color = add(new ColorSetting("color", "Rim color", "ARGB surface and rim opacity.", 0xCC72DFFF));
    final ColorSetting secondary = add(new ColorSetting("secondary", "Tip color", "ARGB color at the cone tip.", 0xDDB890FF));
    final BooleanSetting gradient = add(new BooleanSetting("gradient", "Gradient", "Blend between rim and tip.", true));
    final BooleanSetting outline = add(new BooleanSetting("outline", "Rim outline", "A thin colored band around the brim.", true));
    final DoubleSetting rotation = number("rotation", "Rotation speed", 15, -90, 90);
    public CosmeticHatModule() {
        super("cosmetic_hat", "Cosmetic Hat", "A conical hat on the visible local player's head, in third person only.", Category.GENERAL);
        preset("Prism", "Cool translucent gradient cone.");
        preset("Straw", "Warm opaque straw-like colors.", "color", "#FFD9AF65", "secondary", "#FFF5D791", "rotation", 0);
        preset("Midnight", "Dark cone with a purple rim.", "color", "#BB8A6BFF", "secondary", "#EE23263B", "height", .2);
    }
    private DoubleSetting number(String id, String label, double value, double min, double max) { return add(new DoubleSetting(id, label, "Cosmetic geometry only, local player only.", value, min, max)); }
    public void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new HatFeatureLayer(avatar, this));
        });
    }
}
