package dev.nexvisuals.client.render;

import dev.nexvisuals.client.module.HudModule;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.module.VisualModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Binds registered modules to supported Fabric hooks once, keeping game types out of core. */
public final class HudDispatcher {
    private final ModuleRegistry registry;
    private boolean registered;

    public HudDispatcher(ModuleRegistry registry) {
        this.registry = registry;
    }

    public void register() {
        if (registered) {
            throw new IllegalStateException("HUD hooks are already registered");
        }
        registered = true;
        for (VisualModule module : registry.all()) {
            if (!(module instanceof HudModule hud)) {
                continue;
            }
            Identifier replacement = hud.replacementLayer();
            if (replacement == null) {
                HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                        Identifier.fromNamespaceAndPath("nexvisuals", module.id()), (graphics, deltaTracker) -> {
                            if (hud.isHudActive(module.enabled())) {
                                hud.renderHud(Minecraft.getInstance(), graphics, deltaTracker);
                            }
                        });
            } else {
                HudElementRegistry.replaceElement(replacement, original -> (graphics, deltaTracker) -> {
                    if (hud.isHudActive(module.enabled())) {
                        hud.renderWrapped(Minecraft.getInstance(), graphics, deltaTracker, original);
                    } else {
                        original.render(graphics, deltaTracker);
                    }
                });
            }
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (module.enabled()) {
                    hud.tick(client);
                }
            });
        }
    }
}
