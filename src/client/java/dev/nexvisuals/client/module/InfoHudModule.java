package dev.nexvisuals.client.module;

import dev.nexvisuals.client.hud.TextHudModule;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.setting.BooleanSetting;
import dev.nexvisuals.core.setting.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import java.util.Locale;

/** Independent information elements with shared rendering and placement. No world/entity scans. */
public final class InfoHudModule extends TextHudModule {
    public enum Kind {
        FPS("hud_fps", "FPS", "Local frame rate.", "FPS: 144", 42),
        FACING("hud_facing", "Direction", "Local compass direction, respecting reduced debug info.", "Facing: North", 64),
        SPEED("hud_speed", "Speed", "Local horizontal movement in blocks per second.", "Speed: 0.0 b/s", 86),
        STATUS("hud_status", "Player Status", "Your health, armor and absorption.", "HP: 20 | Armor: 0", 108),
        KEYS("hud_keys", "Keystrokes", "Local movement keys only; no input automation.", "W A S D | Jump", 130);
        final String id, name, description, preview;
        final int y;
        Kind(String id, String name, String description, String preview, int y) {
            this.id = id; this.name = name; this.description = description; this.preview = preview; this.y = y;
        }
    }
    private final Kind kind;
    private int ticks;
    private double lastX, lastZ;
    private Object lastLevel;
    private boolean sampled;
    private final BooleanSetting keyTiles;
    private final ColorSetting pressedColor;
    private final String[] keyLabels = {"W", "A", "S", "D", "Space"};
    private int pressedKeys;
    public InfoHudModule(Kind kind) {
        super(kind.id, kind.name, kind.description, kind.y, kind.preview);
        this.kind = kind;
        keyTiles = kind == Kind.KEYS ? add(new BooleanSetting("key_tiles", "Key tiles", "Readable key caps with pressed-state color instead of idle dashes.", true)) : null;
        pressedColor = kind == Kind.KEYS ? add(new ColorSetting("pressed_color", "Pressed key", "ARGB fill for a key held down.", 0xBB75BFFF)) : null;
    }
    private boolean tiles() { return keyTiles != null && keyTiles.get(); }
    @Override protected int contentWidth(Minecraft client) { return tiles() ? 52 : super.contentWidth(client); }
    @Override protected int contentHeight(Minecraft client) { return tiles() ? 50 : super.contentHeight(client); }
    @Override protected void drawContent(Minecraft client, GuiGraphics g, int color, boolean shadow) {
        if (!tiles()) { super.drawContent(client, g, color, shadow); return; }
        keyTile(client,g,0,23,4,16,16,color,shadow);
        keyTile(client,g,1,5,22,16,16,color,shadow);
        keyTile(client,g,2,23,22,16,16,color,shadow);
        keyTile(client,g,3,41,22,16,16,color,shadow);
        keyTile(client,g,4,5,40,52,14,color,shadow);
    }
    private void keyTile(Minecraft client, GuiGraphics g, int index, int x, int y, int width, int height, int color, boolean shadow) {
        boolean down = (pressedKeys & 1 << index) != 0;
        Draw.roundedRect(g,x,y,width,height,3,down ? pressedColor.get() : 0x66384458);
        String label = keyLabels[index];
        Draw.text(g,client.font,label,x+(width-client.font.width(label))/2,y+(height-client.font.lineHeight)/2,color,shadow);
    }
    @Override protected boolean canDisplay(Minecraft client) {
        return super.canDisplay(client) && (kind != Kind.FACING || !client.player.isReducedDebugInfo());
    }
    @Override public boolean isVisibleInEditor(Minecraft client) { return canDisplay(client); }
    @Override public void tick(Minecraft client) {
        if (!canDisplay(client)) { text = ""; sampled = false; return; }
        ticks++;
        if (kind != Kind.KEYS && kind != Kind.SPEED && !text.isEmpty() && ticks % 5 != 0) return;
        switch (kind) {
            case FPS -> text = "FPS: " + client.getFps();
            case FACING -> {
                int direction = Math.floorMod((int) Math.floor(client.player.getYRot() / 90.0 + .5), 4);
                text = "Facing: " + switch (direction) { case 0 -> "South"; case 1 -> "West"; case 2 -> "North"; default -> "East"; };
            }
            case SPEED -> {
                double x = client.player.getX(), z = client.player.getZ();
                double speed = sampled && lastLevel == client.level ? Math.hypot(x - lastX, z - lastZ) * 20 : 0;
                if (ticks % 5 == 0 || text.isEmpty()) text = String.format(Locale.ROOT, "Speed: %.1f b/s", speed > 100 ? 0 : speed);
                lastX = x; lastZ = z; sampled = true; lastLevel = client.level;
            }
            case STATUS -> text = "HP: " + (int) Math.ceil(client.player.getHealth()) + " | Armor: " + client.player.getArmorValue()
                    + (client.player.getAbsorptionAmount() > 0 ? " | Abs: " + (int) Math.ceil(client.player.getAbsorptionAmount()) : "");
            case KEYS -> {
                var keys = new net.minecraft.client.KeyMapping[] {client.options.keyUp, client.options.keyLeft,
                        client.options.keyDown, client.options.keyRight, client.options.keyJump};
                pressedKeys = 0;
                for (int i=0; i<keys.length; i++) {
                    if (keys[i].isDown()) pressedKeys |= 1 << i;
                    keyLabels[i] = client.font.plainSubstrByWidth(keys[i].getTranslatedKeyMessage().getString(), i==4 ? 48 : 14);
                }
                text = (client.options.keyUp.isDown() ? "W" : "-") + " "
                    + (client.options.keyLeft.isDown() ? "A" : "-") + " "
                    + (client.options.keyDown.isDown() ? "S" : "-") + " "
                    + (client.options.keyRight.isDown() ? "D" : "-") + " | "
                    + (client.options.keyJump.isDown() ? "Jump" : "----");
            }
        }
    }
    @Override protected void onDisable() { super.onDisable(); sampled = false; lastLevel = null; pressedKeys = 0; }
}
