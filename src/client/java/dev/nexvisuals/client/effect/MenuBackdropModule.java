package dev.nexvisuals.client.effect;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.ColorSetting;
import dev.nexvisuals.core.setting.IntSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Changes only vanilla's in-world menu backdrop, leaving menus, slots and tooltips intact. */
public final class MenuBackdropModule extends VisualModule {
    private final ColorSetting color = add(new ColorSetting("color", "Backdrop color", "ARGB background tint for vanilla in-world menus.", 0xA0101828));
    private final IntSetting duration = add(new IntSetting("duration", "Fade duration", "Backdrop fade-in duration in milliseconds.", 160, 0, 700));
    private Screen lastScreen;
    private long opened;
    public MenuBackdropModule() { super("menu_backdrop", "Menu Backdrop", "Color, opacity and a light fade for in-world vanilla menus.", Category.INTERFACE); }
    public void opened(Screen screen) { lastScreen = screen; opened = System.nanoTime(); }
    public void render(Screen screen, GuiGraphics graphics) {
        if (lastScreen != screen) opened(screen);
        double progress = duration.get() == 0 ? 1 : Math.min(1, (System.nanoTime() - opened) / (duration.get() * 1_000_000.0));
        int argb = Draw.withAlpha(color.get(), (float) Easing.OUT_CUBIC.apply(progress));
        Draw.rect(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), argb);
    }
    @Override protected void onDisable() { lastScreen = null; }
}
