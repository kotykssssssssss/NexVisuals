package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.animation.Transition;
import dev.nexvisuals.core.config.GlobalSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Keeps vanilla button keyboard/narration behavior, with a small custom visual treatment. */
final class NexButton extends Button {
    private final GlobalSettings globals;
    private final Transition hover = new Transition(160, Easing.OUT_CUBIC);
    private final Supplier<String> label;
    private final BooleanSupplier selected;
    private GuiLayout.Rect clip;

    NexButton(int x, int y, int width, int height, Supplier<String> label,
              BooleanSupplier selected, Runnable action, GlobalSettings globals) {
        super(x, y, width, height, Component.literal(label.get()), button -> action.run(), DEFAULT_NARRATION);
        this.globals = globals;
        this.label = label;
        this.selected = selected;
    }

    NexButton clipped(GuiLayout.Rect clip) { this.clip = clip; return this; }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return (clip == null || clip.contains(event.x(), event.y())) && super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isMouseOver(double x, double y) {
        return (clip == null || clip.contains(x, y)) && super.isMouseOver(x, y);
    }

    @Override protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        String currentLabel = label.get();
        if (!currentLabel.equals(getMessage().getString())) setMessage(Component.literal(currentLabel));
        hover.setDurationMillis(globals.animationDuration.get());
        hover.target(isHoveredOrFocused() && (isFocused() || clip == null || clip.contains(mouseX, mouseY)));
        int brightness = (int) (hover.value() * 15);
        int background = 0xFF000000 | ((26 + brightness) << 16) | ((34 + brightness) << 8) | (51 + brightness);
        Draw.roundedRect(graphics, getX(), getY(), width, height, 4, selected.getAsBoolean() ? 0xFF303D65 : background);
        if (selected.getAsBoolean()) Draw.rect(graphics, getX() + 2, getY() + 4, 2, height - 8, globals.accentColor.get());
        if (isFocused()) Draw.border(graphics, getX(), getY(), width, height, 1, globals.accentColor.get());
        var font = Minecraft.getInstance().font;
        String text = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, width - 12));
        Draw.text(graphics, font, text, getX() + (width - font.width(text)) / 2,
                getY() + (height - 8) / 2, active ? 0xFFE9EDF7 : 0xFF778198, false);
    }
}
