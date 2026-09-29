package dev.nexvisuals.client.module;

import dev.nexvisuals.client.hud.TextHudModule;
import net.minecraft.client.Minecraft;

/** Local position only. The server's reduced-debug-info restriction is respected. */
public final class HudCoordinatesModule extends TextHudModule {
    private int lastX, lastY, lastZ;
    public HudCoordinatesModule() {
        super("hud_coordinates", "HUD Coordinates", "Your block position in a movable HUD panel.", 20, "XYZ: 0 / 64 / 0");
    }
    @Override protected boolean canDisplay(Minecraft client) {
        return super.canDisplay(client) && !client.player.isReducedDebugInfo();
    }
    @Override public boolean isVisibleInEditor(Minecraft client) { return canDisplay(client); }
    @Override public void tick(Minecraft client) {
        if (!canDisplay(client)) { text = ""; return; }
        int x = client.player.getBlockX(), y = client.player.getBlockY(), z = client.player.getBlockZ();
        if (text.isEmpty() || x != lastX || y != lastY || z != lastZ) {
            lastX = x; lastY = y; lastZ = z;
            text = "XYZ: " + x + " / " + y + " / " + z;
        }
    }
}
