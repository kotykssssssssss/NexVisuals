package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.config.ConfigManager;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.config.ProfileManager;
import dev.nexvisuals.core.config.BuiltinProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

/** Explicit local snapshot operations; no profile files are read during rendering. */
final class ProfilesScreen extends Screen {
    private final Screen parent;
    private final ProfileManager profiles;
    private final GlobalSettings globals;
    private final Runnable applied;
    private List<String> names = List.of();
    private String selected = "";
    private String draftName = "";
    private String status = "Profiles store module and global settings.";
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private ScrollPane list;
    private EditBox name;
    private boolean rebuild;
    private boolean confirmDelete;
    private int builtinIndex;

    ProfilesScreen(Screen parent, ProfileManager profiles, GlobalSettings globals, Runnable applied) {
        super(Component.literal("NexVisuals profiles"));
        this.parent = parent;
        this.profiles = profiles;
        this.globals = globals;
        this.applied = applied;
        refresh();
    }

    private void refresh() {
        try { names = profiles.list(); }
        catch (IOException exception) { status = "Cannot read profiles: " + exception.getMessage(); }
    }

    @Override protected void init() {
        panelWidth = Math.min(560, width - 16);
        panelHeight = Math.min(410, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        BuiltinProfile builtin = BuiltinProfile.values()[builtinIndex];
        addRenderableWidget(button(left + 10, top + 43, panelWidth - 94, builtin.label() + " >", () -> {
            builtinIndex = (builtinIndex + 1) % BuiltinProfile.values().length; rebuild = true;
        }, builtin.description()));
        addRenderableWidget(button(left + panelWidth - 78, top + 43, 68, "Apply style", () -> {
            try {
                var warnings = profiles.applyBuiltin(builtin);
                applied.run();
                status = "Applied " + builtin.label() + (warnings.isEmpty() ? ". Save it as a profile to keep a copy." : " with defaulted fields.");
            } catch (IOException exception) { status = exception.getMessage(); }
        }, "Replace the current visual configuration with this built-in style. Save your own profile first if needed."));
        int inputY = top + 69;
        name = new EditBox(font, left + 10, inputY, panelWidth - 165, 20, Component.literal("Profile name"));
        name.setMaxLength(48);
        name.setHint(Component.literal("Profile name, e.g. clean-pvp"));
        name.setValue(draftName);
        name.setResponder(value -> { draftName = value; name.setTextColor(ProfileManager.validName(value) ? 0xFFE9EDF7 : 0xFFFF939F); });
        addRenderableWidget(name);
        addRenderableWidget(button(left + panelWidth - 149, inputY, 67, "Rename", () -> {
            try {
                profiles.rename(selected, draftName);
                selected = draftName; status = "Renamed to " + selected; refresh(); rebuild = true;
            } catch (IOException | IllegalArgumentException exception) { status = exception.getMessage(); }
        }, "Rename the selected profile to the name entered here. Existing profiles are never overwritten."));
        addRenderableWidget(button(left + panelWidth - 77, inputY, 67, "Save", () -> {
            try {
                profiles.save(draftName);
                status = "Saved profile: " + draftName;
                selected = draftName;
                refresh();
                rebuild = true;
            } catch (IOException | IllegalArgumentException exception) { status = exception.getMessage(); }
        }, "Save current settings. An existing name updates that snapshot."));
        GuiLayout.Rect area = new GuiLayout.Rect(left + 10, top + 97, panelWidth - 20, Math.max(25, panelHeight - 165));
        list = new ScrollPane(area, list == null ? 0 : list.scroll());
        int y = 3;
        for (String profileName : names) {
            NexButton entry = new NexButton(area.x() + 3, 0, area.width() - 10, 22, () -> profileName,
                    () -> selected.equals(profileName), () -> {
                        selected = profileName;
                        draftName = profileName;
                        name.setValue(profileName);
                        confirmDelete = false;
                    }, globals);
            addWidget(entry);
            list.add(entry, y);
            y += 26;
        }
        int actionsY = top + panelHeight - 59;
        int actionWidth = (panelWidth - 38) / 4;
        addRenderableWidget(button(left + 10, actionsY, actionWidth, "Load", () -> {
            try {
                ConfigManager.LoadResult result = profiles.load(selected);
                if (result.status() == ConfigManager.LoadStatus.LOADED) {
                    applied.run();
                    status = "Loaded " + selected + (result.warnings().isEmpty() ? "" : " (some fields used defaults)");
                } else status = "Damaged profile preserved; current settings kept.";
            } catch (IOException | IllegalArgumentException exception) { status = exception.getMessage(); }
        }, "Load the selected profile and apply its visual settings"));
        NexButton delete = new NexButton(left + 16 + actionWidth, actionsY, actionWidth, 20,
                () -> confirmDelete ? "Confirm?" : "Delete", () -> confirmDelete, () -> {
                    if (selected.isEmpty()) { status = "Choose a profile first."; return; }
                    if (!confirmDelete) { confirmDelete = true; status = "Press Confirm? to delete " + selected; return; }
                    try {
                        profiles.delete(selected);
                        status = "Deleted " + selected;
                        selected = "";
                        confirmDelete = false;
                        refresh();
                        rebuild = true;
                    } catch (IOException exception) { status = exception.getMessage(); }
                }, globals);
        addRenderableWidget(delete);
        addRenderableWidget(button(left + 22 + actionWidth * 2, actionsY, actionWidth, "Defaults", () -> {
            profiles.restoreDefaults();
            applied.run();
            status = "Default settings restored. Saved profiles are kept.";
        }, "Restore all module and global settings to defaults; keeps saved profiles"));
        addRenderableWidget(button(left + 28 + actionWidth * 3, actionsY, actionWidth, "Back", this::onClose, "Return to NexVisuals"));
    }

    private NexButton button(int x, int y, int width, String label, Runnable action, String hint) {
        NexButton button = new NexButton(x, y, width, 20, () -> label, () -> false, action, globals);
        button.setTooltip(Tooltip.create(Component.literal(hint)));
        return button;
    }

    @Override public void tick() { if (rebuild) { rebuild = false; rebuildWidgets(); } }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Draw.rect(graphics, 0, 0, width, height, 0xC00A0E18);
        Draw.roundedRect(graphics, left, top, panelWidth, panelHeight, 8, 0xFF101725);
        Draw.text(graphics, font, "LOCAL PROFILES", left + 12, top + 12, globals.accentColor.get(), false);
        Draw.text(graphics, font, "Save a style. Switch whenever you like.", left + 12, top + 27, 0xFF8796B2, false);
        Draw.roundedRect(graphics, list.area.x(), list.area.y(), list.area.width(), list.area.height(), 4, 0xFF0D1421);
        super.render(graphics, mouseX, mouseY, delta);
        list.render(graphics, mouseX, mouseY, delta);
        if (names.isEmpty()) Draw.text(graphics, font, "No profiles yet. Enter a name and save.", left + 18, list.area.y() + 14, 0xFF8796B2, false);
        graphics.drawWordWrap(font, Component.literal(status == null ? "Operation failed" : status),
                left + 10, top + panelHeight - 30, panelWidth - 20, 0xFFB7C4DC);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (list.area.contains(x, y)) { list.scrollBy((int) (-vertical * 26)); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        boolean handled = super.keyPressed(event);
        list.revealFocused();
        return handled;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) { }
    @Override public boolean isPauseScreen() { return false; }
}
