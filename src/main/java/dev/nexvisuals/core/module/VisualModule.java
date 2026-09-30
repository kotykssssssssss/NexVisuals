package dev.nexvisuals.core.module;

import dev.nexvisuals.core.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.google.gson.JsonElement;
import com.google.gson.Gson;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small modules own their settings and optional lifecycle, while hooks live in client code. */
public abstract class VisualModule {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(settings);
    private boolean enabled;
    private final List<ModulePreset> presets = new ArrayList<>();
    private final List<ModuleAction> actions = new ArrayList<>();
    private final List<SettingGroup> groups = new ArrayList<>();
    private long styleRevision = -1;
    private String styleName = "Custom";
    public record SettingGroup(String name, List<Setting<?>> settings) {
        public SettingGroup { settings = List.copyOf(settings); }
    }

    protected VisualModule(String id, String name, String description, Category category) {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid module ID: " + id);
        }
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNullElse(description, "");
        this.category = Objects.requireNonNull(category);
    }

    protected final <S extends Setting<?>> S add(S setting) {
        Objects.requireNonNull(setting);
        if (settings.stream().anyMatch(existing -> existing.id().equals(setting.id()))) {
            throw new IllegalArgumentException("Duplicate setting ID in " + id + ": " + setting.id());
        }
        settings.add(setting);
        return setting;
    }

    public final String id() { return id; }
    public final String name() { return name; }
    public final String description() { return description; }
    public final Category category() { return category; }
    public final List<Setting<?>> settings() { return settingsView; }
    public final List<ModulePreset> presets() { return Collections.unmodifiableList(presets); }
    public final List<ModuleAction> actions() { return Collections.unmodifiableList(actions); }
    public final List<SettingGroup> groups() { return Collections.unmodifiableList(groups); }
    protected final void group(String name, Setting<?>... members) {
        for (var setting : members) if (!settings.contains(setting)) throw new IllegalArgumentException("Unknown group setting");
        groups.add(new SettingGroup(name, List.of(members)));
    }
    /** Optional live information, e.g. why a renderer is temporarily paused. */
    public String runtimeStatus() { return ""; }
    /** Derived from settings, so edits and profile loads cannot leave a misleading preset label. */
    public final String currentPresetName() {
        long revision = 0;
        for (var setting : settings) revision += setting.revision();
        if (revision == styleRevision) return styleName;
        styleRevision = revision;
        styleName = "Custom";
        for (var preset : presets) {
            boolean matches = true;
            for (var setting : settings) {
                JsonElement expected = preset.values().get(setting.id());
                if (preset.values().containsKey(setting.id())) {
                    if (!setting.toJson().equals(expected)) { matches = false; break; }
                } else if (!Objects.equals(setting.get(), setting.defaultValue())) { matches = false; break; }
            }
            if (matches) { styleName = preset.name(); break; }
        }
        return styleName;
    }

    /** Presets are explicit user actions, never side effects of config deserialization. */
    protected final void preset(String name, String description, Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Expected setting/value pairs");
        Map<String, JsonElement> values = new LinkedHashMap<>();
        Gson gson = new Gson();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = (String) pairs[i];
            if (settings.stream().noneMatch(s -> s.id().equals(key))) throw new IllegalArgumentException("Unknown preset setting " + key);
            values.put(key, gson.toJsonTree(pairs[i + 1]));
        }
        presets.add(new ModulePreset(name, description, values));
    }
    protected final void action(String name, String description, Runnable operation) {
        actions.add(new ModuleAction(name, description, operation));
    }
    public final void applyPreset(ModulePreset preset) {
        if (!presets.contains(preset)) throw new IllegalArgumentException("Preset belongs to another module");
        Map<Setting<?>, JsonElement> previous = new LinkedHashMap<>();
        settings.forEach(s -> previous.put(s, s.toJson()));
        try {
            // A named style is a complete starting point; omitted fields use this module's defaults.
            settings.forEach(Setting::reset);
            settings.forEach(s -> { if (preset.values().containsKey(s.id())) s.fromJson(preset.values().get(s.id())); });
        } catch (RuntimeException exception) {
            previous.forEach(Setting::fromJson);
            throw exception;
        }
    }
    public final boolean enabled() { return enabled; }
    public final void toggle() { setEnabled(!enabled); }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        // Commit state only after a lifecycle hook succeeds.
        if (enabled) onEnable(); else onDisable();
        this.enabled = enabled;
    }

    protected void onEnable() { }
    protected void onDisable() { }
}
