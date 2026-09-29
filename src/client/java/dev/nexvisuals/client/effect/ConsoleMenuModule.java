package dev.nexvisuals.client.effect;

import dev.nexvisuals.client.gui.title.ConsoleTitleState;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.hud.ConsoleLayout;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;

/** Appearance only: original TitleScreen actions, restrictions and world loading remain Minecraft-owned. */
public final class ConsoleMenuModule extends VisualModule {
    public final EnumSetting<ConsoleLayout.Alignment> alignment = add(new EnumSetting<>("alignment", "Panel position", "Place the console-style menu on the left, center or right.", ConsoleLayout.Alignment.CENTER, ConsoleLayout.Alignment.class));
    public final EnumSetting<MenuMotion.Style> motion = add(new EnumSetting<>("motion", "Panorama motion", "Still, full orbit, gentle side-to-side sway or drifting orbit.", MenuMotion.Style.DRIFT, MenuMotion.Style.class));
    public final DoubleSetting speed = add(new DoubleSetting("speed", "Movement speed", "Multiplier for menu motion; zero freezes it. Respects Minecraft's Panorama Scroll Speed.", .7, 0, 3));
    public final DoubleSetting amplitude = add(new DoubleSetting("amplitude", "Sway / drift amount", "Angular range of sway and drift.", 14, 0, 35));
    public final DoubleSetting pitch = add(new DoubleSetting("pitch", "Panorama tilt", "Menu panorama angle only.", 10, -25, 25));
    public final DoubleSetting yaw = add(new DoubleSetting("yaw", "Starting direction", "Starting panorama heading in degrees.", 0, -180, 180));
    public final BooleanSetting reverse = add(new BooleanSetting("reverse", "Reverse direction", "Reverse the movement of the menu background.", false));
    public final BooleanSetting reduceMotion = add(new BooleanSetting("reduce_motion", "Reduced motion", "Freeze panorama and decoration; disable button entrance and hover transitions.", false));
    public final IntSetting entrance = add(new IntSetting("entrance", "Button entrance", "Slide-in time in milliseconds. Zero disables entrance movement.", 350, 0, 1000));
    public final ColorSetting accent = add(new ColorSetting("accent", "Accent color", "Selected button, frame and loading decoration.", 0xFFA9C979));
    public final ColorSetting panel = add(new ColorSetting("panel", "Stone panel color", "ARGB tint and opacity of the block-shaped panel.", 0xDA262922));
    public final ColorSetting text = add(new ColorSetting("text", "Text color", "Button labels; vanilla Minecraft font is retained.", 0xFFF4F0DD));
    public final ColorSetting skyTint = add(new ColorSetting("sky_tint", "Panorama tint", "ARGB overlay on the original resource-pack panorama; alpha controls tint strength.", 0x183D6134));
    public final BooleanSetting pixels = add(new BooleanSetting("pixels", "Drifting pixels", "A few slow, block-shaped motes over the panorama.", true));
    public final BooleanSetting loading = add(new BooleanSetting("loading", "World loading theme", "Theme ordinary world loading; keep real progress, narration and portal screens.", true));
    private final MenuClock clock = new MenuClock();
    private ConsoleTitleState titleState;

    public ConsoleMenuModule() {
        super("console_menu", "Console Menu", "Animated vanilla-style title menu inspired by classic console editions. Changes apply on returning to the title screen.", Category.INTERFACE);
        preset("Classic", "Stone buttons, moss accents and a gently drifting vanilla panorama.");
        preset("Sunset", "Warm amber panels with a slow sideways camera.", "accent", "#FFFFC477", "panel", "#DA352B22", "sky_tint", "#35CB722C", "motion", "SWAY", "speed", .55);
        preset("Moonlight", "Blue-gray stone and a quiet orbit.", "accent", "#FFA3CEFF", "panel", "#DF202B3C", "sky_tint", "#44274477", "motion", "ORBIT", "speed", .35);
        preset("Still", "Classic colors with all decorative motion disabled.", "reduce_motion", true);
    }
    public ConsoleTitleState titleState(Screen screen) { return titleState != null && titleState.screen()==screen ? titleState : null; }
    public void attach(ConsoleTitleState state) { titleState=state; }
    public void detach(Screen screen) { if (titleState(screen)!=null) titleState=null; }
    public boolean affectsPanorama(Screen screen) {
        return enabled() && (screen instanceof TitleScreen || loading.get() && screen instanceof LevelLoadingScreen);
    }
    public double phase() {
        double vanillaSpeed = Minecraft.getInstance().options.panoramaSpeed().get();
        return clock.advance(speed.get()*vanillaSpeed, !reduceMotion.get() && motion.get()!=MenuMotion.Style.STILL);
    }
    public MenuMotion.Angles angles() { return MenuMotion.angles(motion.get(), phase(), amplitude.get(), pitch.get(), yaw.get(), reverse.get()); }
    @Override protected void onDisable() { titleState=null; }
}
