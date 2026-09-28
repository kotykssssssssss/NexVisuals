package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.hud.EditableHud;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.module.VisualModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Edits the same position settings used by HUD rendering; previews never enable modules implicitly. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private final GlobalSettings globals;
    private final Runnable save;
    private final List<VisualModule> widgets;
    private VisualModule selected;
    private VisualModule dragging;
    private double grabX;
    private double grabY;
    private boolean grid = true;

    public HudEditorScreen(Screen parent, ModuleRegistry registry, GlobalSettings globals, Runnable save) {
        super(Component.literal("NexVisuals HUD editor"));
        this.parent = parent;
        this.globals = globals;
        this.save = save;
        widgets = registry.all().stream().filter(module -> module instanceof EditableHud).toList();
    }

    @Override protected void init() {
        addRenderableWidget(new NexButton(width - 72, 8, 64, 20, () -> "Done", () -> false, this::onClose, globals));
        NexButton gridButton = new NexButton(width - 142, 8, 64, 20, () -> grid ? "Grid: ON" : "Grid: OFF",
                () -> grid, () -> grid = !grid, globals);
        gridButton.setTooltip(Tooltip.create(Component.literal("Toggle alignment guides. Hold Shift while dragging to snap to 8 GUI pixels.")));
        addRenderableWidget(gridButton);
        NexButton enabled = new NexButton(8, height - 28, 82, 20,
                () -> selected == null ? "Select a HUD" : selected.enabled() ? "Enabled" : "Disabled",
                () -> selected != null && selected.enabled(), () -> { if (selected != null) selected.toggle(); }, globals);
        enabled.setTooltip(Tooltip.create(Component.literal("Enable or disable the selected HUD element. Right-clicking its preview also toggles it.")));
        addRenderableWidget(enabled);
        NexButton settings = new NexButton(96, height - 28, 72, 20, () -> "Settings", () -> false, () -> {
            if (selected != null && parent instanceof NexVisualsScreen configScreen) {
                configScreen.selectModule(selected.id());
                onClose();
            }
        }, globals);
        settings.setTooltip(Tooltip.create(Component.literal("Open the selected module's appearance, scale and anchor settings")));
        addRenderableWidget(settings);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Draw.rect(graphics, 0, 0, width, height, 0x640A0E18);
        if (grid) {
            for (int x = 0; x < width; x += 16) Draw.rect(graphics, x, 0, 1, height, 0x133D5A8A);
            for (int y = 0; y < height; y += 16) Draw.rect(graphics, 0, y, width, 1, 0x133D5A8A);
            Draw.rect(graphics, width / 2, 0, 1, height, 0x553D5A8A);
            Draw.rect(graphics, 0, height / 2, width, 1, 0x553D5A8A);
        }
        for (VisualModule module : widgets) {
            EditableHud hud = (EditableHud) module;
            if (!hud.isVisibleInEditor(minecraft)) continue;
            hud.renderPreview(minecraft, graphics);
            EditableHud.Bounds bounds = hud.bounds(minecraft, width, height);
            int border = module == selected ? globals.accentColor.get()
                    : bounds.contains(mouseX, mouseY) ? 0xFFB9CAEE : 0xAA657692;
            Draw.border(graphics, bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, 1, border);
            if (bounds.contains(mouseX, mouseY) || module == selected) {
                String label = module.name() + (module.enabled() ? "" : " (disabled preview)");
                int labelY = bounds.y() >= 12 ? bounds.y() - 11 : bounds.y() + bounds.height() + 3;
                int labelX = Math.clamp(bounds.x(), 0, Math.max(0, width - font.width(label)));
                Draw.text(graphics, font, label, labelX, labelY, 0xFFE5EDFF, true);
            }
        }
        Draw.roundedRect(graphics, 4, 4, width - 8, 30, 5, 0xEC101725);
        Draw.text(graphics, font, "HUD EDITOR", 12, 10, globals.accentColor.get(), false);
        Draw.text(graphics, font, font.plainSubstrByWidth("Drag to move / right-click to toggle", Math.max(30, width - 165)), 12, 22, 0xFF8796B2, false);
        Draw.roundedRect(graphics, 4, height - 33, width - 8, 29, 5, 0xEC101725);
        if (width > 430) Draw.text(graphics, font, "Arrows: 1 px / Shift: 8 px / saved on close", 178, height - 21, 0xFF8796B2, false);
        if (widgets.isEmpty()) graphics.drawCenteredString(font, "No movable HUD modules are registered.", width / 2, height / 2, 0xFFB7C4DC);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.y() < 35 || event.y() >= height - 34) return false;
        for (int i = widgets.size() - 1; i >= 0; i--) {
            VisualModule module = widgets.get(i);
            EditableHud hud = (EditableHud) module;
            if (!hud.isVisibleInEditor(minecraft)) continue;
            EditableHud.Bounds bounds = hud.bounds(minecraft, width, height);
            if (!bounds.contains(event.x(), event.y())) continue;
            selected = module;
            clearFocus();
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) module.toggle();
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                dragging = module;
                grabX = event.x() - bounds.x();
                grabY = event.y() - bounds.y();
            }
            return true;
        }
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int x = (int) Math.round(event.x() - grabX);
            int y = (int) Math.round(event.y() - grabY);
            if ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) { x = Math.round(x / 8f) * 8; y = Math.round(y / 8f) * 8; }
            ((EditableHud) dragging).moveTo(x, y, width, height);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) { dragging = null; return true; }
        return super.mouseReleased(event);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (selected != null && getFocused() == null) {
            int step = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 8 : 1;
            int dx = event.key() == GLFW.GLFW_KEY_LEFT ? -step : event.key() == GLFW.GLFW_KEY_RIGHT ? step : 0;
            int dy = event.key() == GLFW.GLFW_KEY_UP ? -step : event.key() == GLFW.GLFW_KEY_DOWN ? step : 0;
            if (dx != 0 || dy != 0) {
                EditableHud hud = (EditableHud) selected;
                EditableHud.Bounds bounds = hud.bounds(minecraft, width, height);
                hud.moveTo(bounds.x() + dx, bounds.y() + dy, width, height);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void removed() { save.run(); }
    @Override public boolean isPauseScreen() { return false; }
}
