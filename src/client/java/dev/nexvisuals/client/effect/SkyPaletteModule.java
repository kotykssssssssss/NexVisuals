package dev.nexvisuals.client.effect;

import dev.nexvisuals.core.animation.EffectMath;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;

/** Edits only extracted sky colors. No world time, lighting, fog or visibility modifications. */
public final class SkyPaletteModule extends VisualModule {
    private final ColorSetting sky = add(new ColorSetting("sky", "Sky color", "RGB tint of the vanilla overworld sky disc.", 0xFF4B397A));
    private final ColorSetting horizon = add(new ColorSetting("horizon", "Horizon accent", "ARGB sunrise/sunset band. Does not affect terrain or fog.", 0x7072A6E8));
    private final DoubleSetting strength = add(new DoubleSetting("strength", "Blend", "Blend from the real vanilla sky to the chosen palette. Zero is vanilla.", .7, 0, 1));
    private final boolean irisPresent = FabricLoader.getInstance().isModLoaded("iris");
    public SkyPaletteModule() {
        super("sky_palette", "Sky Palette", "Vanilla sky colors only. Automatically inactive when Iris is installed to avoid owning its sky.", Category.WORLD);
        preset("Vanilla", "Keep the real sky unchanged.", "strength", 0);
        preset("Night blue", "Deep blue palette; time, sun and lighting stay real.", "sky", "#FF142644", "horizon", "#30334170");
        preset("Sunset", "Warm sky and peach horizon.", "sky", "#FF805A80", "horizon", "#B0FF995E");
        preset("Dark", "Muted navy background.", "sky", "#FF131922", "horizon", "#302A364B", "strength", .85);
        preset("Cosmic", "Purple and cool blue sky colors.");
    }
    public void apply(SkyRenderState state) {
        if (!enabled() || irisPresent || state.skybox != DimensionType.Skybox.OVERWORLD) return;
        state.skyColor = EffectMath.color(state.skyColor, sky.get(), strength.get());
        state.sunriseAndSunsetColor = EffectMath.color(state.sunriseAndSunsetColor, horizon.get(), strength.get());
    }
}
