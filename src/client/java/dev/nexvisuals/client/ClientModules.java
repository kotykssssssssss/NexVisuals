package dev.nexvisuals.client;

import dev.nexvisuals.client.cosmetic.*;
import dev.nexvisuals.client.effect.*;
import dev.nexvisuals.client.hud.VanillaHudModule;
import dev.nexvisuals.client.hud.EquipmentHudModule;
import dev.nexvisuals.client.hud.ItemCounterModule;
import dev.nexvisuals.client.hud.StatusEffectsHudModule;
import dev.nexvisuals.client.hud.ActiveModulesHudModule;
import dev.nexvisuals.client.interfacefx.ContainerVisualsModule;
import dev.nexvisuals.client.module.*;
import dev.nexvisuals.client.particle.*;
import dev.nexvisuals.client.sky.SkyboxModule;
import dev.nexvisuals.core.module.ModuleRegistry;

/** One explicit registration list, with no hook registration or window access during construction. */
public final class ClientModules {
    public final ModuleRegistry registry = new ModuleRegistry();
    public final ViewmodelModule viewmodel = registry.register(new ViewmodelModule());
    public final SwingModule swing = registry.register(new SwingModule());
    public final ShieldModule shield = registry.register(new ShieldModule());
    public final CameraModule camera = registry.register(new CameraModule());
    public final MenuBackdropModule backdrop = registry.register(new MenuBackdropModule());
    public final ConsoleMenuModule consoleMenu = registry.register(new ConsoleMenuModule());
    public final dev.nexvisuals.client.background.LiveBackgroundModule liveBackground = registry.register(new dev.nexvisuals.client.background.LiveBackgroundModule());
    public final FireOverlayModule fire = registry.register(new FireOverlayModule());
    public final SkyPaletteModule sky = registry.register(new SkyPaletteModule());
    public final SkyboxModule skybox = registry.register(new SkyboxModule());
    public final dev.nexvisuals.client.post.PostProcessingModule post = registry.register(new dev.nexvisuals.client.post.PostProcessingModule());
    public final ContainerVisualsModule containers = registry.register(new ContainerVisualsModule());
    public final CosmeticParticlesModule classicParticles = registry.register(new CosmeticParticlesModule());
    public final EffectEmitter emitter = new EffectEmitter();
    public final JumpRingsModule jumpRings = registry.register(new JumpRingsModule(emitter));
    public final FootstepEffectsModule footsteps = registry.register(new FootstepEffectsModule(emitter));
    public final TotemEchoModule totemEcho = registry.register(new TotemEchoModule(emitter));
    public final BlockEffectsModule blockEffects = registry.register(new BlockEffectsModule(emitter));
    public final OrbitalsModule orbitals = registry.register(new OrbitalsModule(emitter));
    public final dev.nexvisuals.client.post.WeatherLensModule weatherLens = registry.register(new dev.nexvisuals.client.post.WeatherLensModule());
    public final dev.nexvisuals.client.post.UnderwaterEffectsModule underwater = registry.register(new dev.nexvisuals.client.post.UnderwaterEffectsModule());
    public final dev.nexvisuals.client.post.RetroDisplayModule retroDisplay = registry.register(new dev.nexvisuals.client.post.RetroDisplayModule());
    public final HitVisualsModule hits = registry.register(new HitVisualsModule(classicParticles, emitter));
    public final HitSoundsModule sounds = registry.register(new HitSoundsModule());
    public final PlayerTrailsModule trails = registry.register(new PlayerTrailsModule(emitter));
    public final FirefliesModule fireflies = registry.register(new FirefliesModule(emitter));
    public final ElytraTrailsModule elytraTrails = registry.register(new ElytraTrailsModule(emitter));
    public final CosmeticHatModule hat = registry.register(new CosmeticHatModule());
    public final VanillaHudModule hotbar = registry.register(new VanillaHudModule(VanillaHudModule.Element.HOTBAR, null));
    public ClientModules() {
        post.attachEffects(weatherLens,underwater,retroDisplay);
        registry.register(new CustomCrosshairModule());
        registry.register(new HudCoordinatesModule());
        registry.register(new EquipmentHudModule());
        registry.register(new ItemCounterModule());
        registry.register(new StatusEffectsHudModule());
        registry.register(new ActiveModulesHudModule(registry));
        for (InfoHudModule.Kind kind : InfoHudModule.Kind.values()) registry.register(new InfoHudModule(kind));
        registry.register(new ScreenTintModule());
        for (VanillaHudModule.Element element : VanillaHudModule.Element.values())
            if (element != VanillaHudModule.Element.HOTBAR) registry.register(new VanillaHudModule(element, hotbar));
    }
}
