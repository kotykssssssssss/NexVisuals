package dev.nexvisuals.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nexvisuals.client.gui.NexVisualsScreen;
import dev.nexvisuals.client.module.CustomCrosshairModule;
import dev.nexvisuals.client.module.HudCoordinatesModule;
import dev.nexvisuals.client.render.HudDispatcher;
import dev.nexvisuals.core.config.ConfigManager;
import dev.nexvisuals.core.config.GlobalSettings;
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
    private final ModuleRegistry modules = new ModuleRegistry();
    private final GlobalSettings globals = new GlobalSettings();
    private ConfigManager config;
    private boolean canSave;

    @Override
    public void onInitializeClient() {
        modules.register(new CustomCrosshairModule());
        modules.register(new HudCoordinatesModule());
        config = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("nexvisuals.json"), modules, globals);
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
        KeyMapping openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nexvisuals.open_settings", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT,
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "controls"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) {
                if (client.screen == null) openScreen(client, null);
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
                Screens.getButtons(screen).add(Button.builder(Component.literal("NexVisuals"), button -> openScreen(client, screen))
                        .bounds(width - 104, 6, 98, 20).build());
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> save());
        LOGGER.info("NexVisuals initialized: Minecraft 1.21.11, {} visual modules", modules.all().size());
    }

    private void openScreen(Minecraft client, Screen parent) {
        client.setScreen(new NexVisualsScreen(parent, modules, globals, this::save));
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
