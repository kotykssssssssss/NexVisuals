package dev.nexvisuals.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nexvisuals.client.gui.NexVisualsScreen;
import dev.nexvisuals.client.gui.title.ConsoleTitleState;
import dev.nexvisuals.client.effect.*;
import dev.nexvisuals.client.interfacefx.ContainerVisualsModule;
import dev.nexvisuals.client.render.HudDispatcher;
import dev.nexvisuals.core.config.ConfigManager;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.config.ProfileManager;
import dev.nexvisuals.core.module.ModuleRegistry;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wires the client services once; individual modules own their behavior and settings. */
public final class NexVisualsClient implements ClientModInitializer {
    public static final String MOD_ID = "nexvisuals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static NexVisualsClient instance;
    private final ClientModules catalog = new ClientModules();
    private final ModuleRegistry modules = catalog.registry;
    private final GlobalSettings globals = new GlobalSettings();
    private ConfigManager config;
    private ProfileManager profiles;
    private boolean canSave;

    // Narrow mixin adapters share the same module instances as GUI/config and Fabric hooks.
    public static NexVisualsClient instance() { return instance; }
    public ViewmodelModule viewmodel() { return catalog.viewmodel; }
    public CameraModule camera() { return catalog.camera; }
    public MenuBackdropModule backdrop() { return catalog.backdrop; }
    public ConsoleMenuModule consoleMenu() { return catalog.consoleMenu; }
    public SwingModule swing() { return catalog.swing; }
    public ShieldModule shield() { return catalog.shield; }
    public FireOverlayModule fire() { return catalog.fire; }
    public SkyPaletteModule sky() { return catalog.sky; }
    public dev.nexvisuals.client.sky.SkyboxModule skybox() { return catalog.skybox; }
    public dev.nexvisuals.client.post.PostProcessingModule post() { return catalog.post; }
    public ContainerVisualsModule containers() { return catalog.containers; }

    @Override
    public void onInitializeClient() {
        instance = this;
        LocalAttackFeedback.register(catalog.hits::attacked, catalog.sounds::attacked);
        ClientTickEvents.END_CLIENT_TICK.register(catalog.trails::tick);
        ClientTickEvents.END_CLIENT_TICK.register(catalog.fireflies::tick);
        ClientTickEvents.END_CLIENT_TICK.register(catalog.elytraTrails::tick);
        catalog.hat.register();
        config = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("nexvisuals.json"), modules, globals);
        profiles = new ProfileManager(FabricLoader.getInstance().getConfigDir().resolve("nexvisuals/profiles"), modules, globals);
        try {
            ConfigManager.LoadResult loaded = config.load();
            canSave = true;
            loaded.warnings().forEach(LOGGER::warn);
            if (loaded.backup() != null) LOGGER.warn("Preserved malformed configuration at {}", loaded.backup());
        } catch (IOException exception) {
            // A read/backup failure must not turn into an accidental overwrite at shutdown.
            LOGGER.error("Cannot load NexVisuals configuration; saving is disabled for this session", exception);
        }
        new HudDispatcher(modules).register();
        catalog.hotbar.registerCompanionLayers();
        KeyMapping openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nexvisuals.open_settings", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "controls"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) {
                if (client.player != null && client.level != null && client.screen == null) openScreen(client, null);
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            catalog.backdrop.opened(screen);
            if (screen instanceof TitleScreen title) {
                if (catalog.consoleMenu.enabled()) {
                    catalog.consoleMenu.attach(new ConsoleTitleState(title, catalog.consoleMenu,
                            () -> openMenuAppearance(client, title), () -> {
                                catalog.consoleMenu.setEnabled(false); save(); client.setScreen(new TitleScreen());
                            }));
                    ScreenEvents.remove(screen).register(catalog.consoleMenu::detach);
                } else {
                    Screens.getButtons(screen).add(Button.builder(Component.translatable("nexvisuals.menu.enable"), button -> {
                        catalog.consoleMenu.setEnabled(true); save(); client.setScreen(new TitleScreen());
                    }).bounds(6, 6, Math.min(150, width-12), 20).build());
                }
            }
            if (client.level != null && screen instanceof PauseScreen) {
                Screens.getButtons(screen).add(Button.builder(Component.literal("NexVisuals"), button -> openScreen(client, screen))
                        .bounds(6, 6, 98, 20).build());
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> { catalog.skybox.closeRenderer(); catalog.post.closeRenderer(); save(); });
        LOGGER.info("NexVisuals initialized: Minecraft 1.21.11, {} visual modules", modules.all().size());
    }

    private void openScreen(Minecraft client, Screen parent) {
        client.setScreen(new NexVisualsScreen(parent, modules, globals, profiles, this::save));
    }

    private void openMenuAppearance(Minecraft client, Screen parent) {
        NexVisualsScreen settings = new NexVisualsScreen(parent, modules, globals, profiles, this::save);
        settings.selectModule("console_menu");
        client.setScreen(settings);
    }

    private void save() {
        if (!canSave) return;
        try {
            config.save();
        } catch (IOException exception) {
            LOGGER.error("Cannot save NexVisuals configuration", exception);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) client.player.displayClientMessage(
                    Component.literal("NexVisuals: settings could not be saved. See the log."), false);
        }
    }
}
