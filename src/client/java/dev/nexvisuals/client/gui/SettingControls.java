package dev.nexvisuals.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** This is the only type-to-control mapping. New modules never require GUI-specific code. */
final class SettingControls {
    private final Font font;
    private final GlobalSettings globals;
    private final GuiState state;
    private final Consumer<AbstractWidget> register;
    private final Consumer<KeybindSetting> captureKey;
    private final Runnable rebuild;

    SettingControls(Font font, GlobalSettings globals, GuiState state, Consumer<AbstractWidget> register,
                    Consumer<KeybindSetting> captureKey, Runnable rebuild) {
        this.font = font;
        this.globals = globals;
        this.state = state;
        this.register = register;
        this.captureKey = captureKey;
        this.rebuild = rebuild;
    }

    int add(ScrollPane pane, Setting<?> setting, int y) {
        int x = pane.area.x() + 8;
        int width = pane.area.width() - 16;
        int rowHeight = setting instanceof ColorSetting ? 77 : 53;
        pane.decorate(y, rowHeight, (graphics, rowY) -> {
            Draw.roundedRect(graphics, x - 3, rowY, width + 6, rowHeight - 2, 4, Draw.withAlpha(0xFF172132, globals.panelOpacity.get().floatValue()));
            Draw.text(graphics, font, font.plainSubstrByWidth(setting.name(), width - 30), x + 3, rowY + 6, 0xFFC6D0E4, false);
            if (setting instanceof ColorSetting color) {
                Draw.rect(graphics, x + width - 43, rowY + 5, 12, 9, color.get());
            }
        });
        NexButton reset = new NexButton(x + width - 23, 0, 23, 15, () -> "R", () -> false,
                () -> { setting.reset(); state.drafts.remove(setting); state.invalidDrafts.remove(setting); rebuild.run(); }, globals);
        reset.setTooltip(Tooltip.create(Component.literal("Reset " + setting.name() + " to its default")));
        add(pane, reset, y + 3);
        int controlY = y + 25;
        AbstractWidget control;
        if (setting instanceof BooleanSetting bool) {
            control = new NexButton(x, 0, width, 20, () -> bool.get() ? "Enabled" : "Disabled", bool::get,
                    () -> bool.set(!bool.get()), globals);
        } else if (setting instanceof IntSetting number) {
            control = new NumericSlider(x, width, number.min(), number.max(), () -> number.get(),
                    value -> number.set((int) Math.round(value)), true, pane.area, globals);
        } else if (setting instanceof DoubleSetting number) {
            control = new NumericSlider(x, width, number.min(), number.max(), number::get,
                    number::set, false, pane.area, globals);
        } else if (setting instanceof EnumSetting<?> choice) {
            control = new NexButton(x, 0, width, 20, () -> choice.get().toString().replace('_', ' '), () -> false,
                    choice::cycle, globals);
        } else if (setting instanceof KeybindSetting keybind) {
            control = new NexButton(x, 0, width, 20, () -> keyName(keybind.get()), () -> false,
                    () -> captureKey.accept(keybind), globals);
        } else if (setting instanceof ColorSetting color) {
            control = editor(pane, x, width, setting, color.hex(), 9, color::setHex);
        } else if (setting instanceof TextSetting text) {
            control = editor(pane, x, width, setting, text.get(), text.maxLength(), text::set);
        } else if (setting instanceof RangeSetting range) {
            // One atomic range editor avoids applying half of an unfinished lower/upper pair.
            String initial = String.format(Locale.ROOT, "%s, %s", range.get().lower(), range.get().upper());
            control = editor(pane, x, width, setting, initial, 64, value -> {
                String[] parts = value.split(",", -1);
                if (parts.length != 2) throw new IllegalArgumentException("Use lower, upper");
                range.set(new DoubleRange(Double.parseDouble(parts[0].strip()), Double.parseDouble(parts[1].strip())));
            });
        } else {
            control = new NexButton(x, 0, width, 20, () -> "Unsupported setting type", () -> false, () -> {}, globals);
            control.active = false;
        }
        String hint = setting.description();
        if (setting instanceof ColorSetting) hint += " Format: #AARRGGBB (alpha, red, green, blue).";
        if (setting instanceof RangeSetting range) hint += " Enter lower, upper. Bounds: " + range.min() + " to " + range.max() + ".";
        if (setting instanceof IntSetting number) hint += " Range: " + number.min() + " to " + number.max() + ". Use arrow keys for precise changes.";
        if (setting instanceof DoubleSetting number) hint += " Range: " + number.min() + " to " + number.max() + ".";
        control.setTooltip(Tooltip.create(Component.literal(hint)));
        add(pane, control, controlY);
        if (setting instanceof ColorSetting color) {
            int[] swatches = {0xF4F7FF, 0x72DFFF, 0xB298FF, 0x71E6B5, 0xFFD080, 0xFF7F9E};
            int swatchWidth = Math.min(25, (width-10)/6);
            for (int i = 0; i < swatches.length; i++) {
                int rgb = swatches[i];
                var swatch = new ColorSwatch(x + i*(swatchWidth+2), swatchWidth, rgb, pane.area, () -> {
                    color.set((color.get() & 0xFF000000) | rgb); state.drafts.remove(setting); state.invalidDrafts.remove(setting); rebuild.run();
                });
                swatch.setTooltip(Tooltip.create(Component.literal(String.format(Locale.ROOT, "#%06X — keep current alpha", rgb))));
                add(pane, swatch, y + 51);
            }
        }
        return y + rowHeight + 5;
    }

    private static final class ColorSwatch extends net.minecraft.client.gui.components.Button {
        private final int rgb;
        private final GuiLayout.Rect clip;
        ColorSwatch(int x, int width, int rgb, GuiLayout.Rect clip, Runnable action) {
            super(x, 0, width, 17, Component.literal(String.format(Locale.ROOT, "Color #%06X", rgb)), b -> action.run(), DEFAULT_NARRATION);
            this.rgb = rgb;
            this.clip = clip;
        }
        @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return clip.contains(event.x(), event.y()) && super.mouseClicked(event, doubleClick); }
        @Override public boolean isMouseOver(double x, double y) { return clip.contains(x, y) && super.isMouseOver(x, y); }
        @Override protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            Draw.roundedRect(graphics, getX()+2, getY()+2, width-4, height-4, 4, 0xFF000000 | rgb);
            if (isHoveredOrFocused()) Draw.border(graphics, getX(), getY(), width, height, 1, 0xFFEDF5FF);
        }
    }

    private void add(ScrollPane pane, AbstractWidget widget, int offset) {
        register.accept(widget);
        pane.add(widget, offset);
    }

    private EditBox editor(ScrollPane pane, int x, int width, Setting<?> setting, String initial,
                           int maxLength, Consumer<String> setter) {
        EditBox editor = new ClippedEditBox(font, x, 0, width, Component.literal(setting.name()), pane.area);
        editor.setMaxLength(maxLength);
        editor.setValue(state.drafts.getOrDefault(setting, initial));
        editor.setTextColor(0xFFE9EDF7);
        editor.setResponder(value -> {
            state.drafts.put(setting, value);
            try {
                setter.accept(value);
                state.invalidDrafts.remove(setting);
                editor.setTextColor(0xFFE9EDF7);
            } catch (IllegalArgumentException exception) {
                state.invalidDrafts.add(setting);
                editor.setTextColor(0xFFFF939F);
            }
        });
        editor.setValue(editor.getValue());
        return editor;
    }

    static String keyName(String translationKey) {
        try { return InputConstants.getKey(translationKey).getDisplayName().getString(); }
        catch (IllegalArgumentException exception) { return translationKey; }
    }

    private static final class NumericSlider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        private final boolean integral;
        private final GuiLayout.Rect clip;
        private final GlobalSettings globals;

        NumericSlider(int x, int width, double min, double max, DoubleSupplier getter, DoubleConsumer setter,
                      boolean integral, GuiLayout.Rect clip, GlobalSettings globals) {
            super(x, 0, width, 20, Component.empty(), max == min ? 0 : (getter.getAsDouble() - min) / (max - min));
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
            this.integral = integral;
            this.clip = clip;
            this.globals = globals;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.literal(integral ? Integer.toString((int) getter.getAsDouble())
                    : String.format(Locale.ROOT, "%.2f", getter.getAsDouble())));
        }
        @Override protected void applyValue() { setter.accept(min + value * (max - min)); }
        @Override public boolean keyPressed(KeyEvent event) {
            if (canChangeValue && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT)) {
                double step = integral ? 1 : 0.01;
                if ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) step *= 10;
                double next = getter.getAsDouble() + (event.key() == GLFW.GLFW_KEY_LEFT ? -step : step);
                setValue(max == min ? 0 : (next - min) / (max - min));
                return true;
            }
            return super.keyPressed(event);
        }
        @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return clip.contains(event.x(), event.y()) && super.mouseClicked(event, doubleClick);
        }
        @Override public boolean isMouseOver(double x, double y) { return clip.contains(x, y) && super.isMouseOver(x, y); }
        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            Draw.roundedRect(graphics, getX(), getY(), width, height, 3, 0xFF222E43);
            int fill = (int) Math.round((width - 6) * value);
            Draw.roundedRect(graphics, getX() + 3, getY() + 14, Math.max(2, fill), 3, 1, globals.accentColor.get());
            if (isFocused()) Draw.border(graphics, getX(), getY(), width, height, 1, globals.accentColor.get());
            graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + 3, 0xFFE9EDF7);
        }
    }
}
