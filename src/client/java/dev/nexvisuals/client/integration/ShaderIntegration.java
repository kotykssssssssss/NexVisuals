package dev.nexvisuals.client.integration;

import dev.nexvisuals.client.NexVisualsClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Optional adapter to the public, mapping-independent Iris API. No pack files/private settings are touched. */
public final class ShaderIntegration {
    private ShaderIntegration() { }
    public static boolean available() { return FabricLoader.getInstance().isModLoaded("iris"); }
    public static void openSettings(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        if (!available()) return;
        try {
            // Reflection only on explicit user action: Iris remains an optional, unbundled dependency.
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Object screen = api.getMethod("openMainIrisScreenObj", Object.class).invoke(instance, parent);
            if (!(screen instanceof Screen result)) throw new IllegalStateException("Iris did not return a Minecraft screen");
            client.setScreen(result);
        } catch (ReflectiveOperationException | LinkageError | IllegalStateException exception) {
            NexVisualsClient.LOGGER.warn("The installed Iris public API could not open its settings", exception);
            if (client.player != null) client.player.displayClientMessage(Component.literal("NexVisuals: open Iris settings from Minecraft Video Settings. See log for details."), false);
        }
    }
}
