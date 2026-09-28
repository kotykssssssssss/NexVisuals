package dev.nexvisuals.client.module;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

/** Optional client-side behavior for a VisualModule that draws a HUD element. */
public interface HudModule {
    /** Null adds a new overlay before chat; an identifier wraps that existing HUD layer. */
    default Identifier replacementLayer() {
        return null;
    }

    default void tick(Minecraft client) {
    }

    /** Return false to let a wrapped vanilla layer render unchanged. */
    boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker);

    default void renderWrapped(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker,
                               HudElement original) {
        if (!renderHud(client, graphics, deltaTracker)) {
            original.render(graphics, deltaTracker);
        }
    }
}
