package dev.nexvisuals.core.module;

import dev.nexvisuals.core.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Small modules own their settings and optional lifecycle, while hooks live in client code. */
public abstract class VisualModule {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(settings);
    private boolean enabled;

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
