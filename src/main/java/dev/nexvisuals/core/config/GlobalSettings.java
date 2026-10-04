package dev.nexvisuals.core.config;

import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.setting.BooleanSetting;
import dev.nexvisuals.core.setting.ColorSetting;
import dev.nexvisuals.core.setting.IntSetting;
import dev.nexvisuals.core.setting.DoubleSetting;
import dev.nexvisuals.core.setting.Setting;
import dev.nexvisuals.core.setting.EnumSetting;
import dev.nexvisuals.core.visual.ParticleBudget;
import dev.nexvisuals.core.ui.PanelPosition;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GlobalSettings {
    public final IntSetting animationDuration = new IntSetting("animation_duration", "Animation duration",
            "Transition duration in milliseconds; zero disables motion.", 160, 0, 1000);
    public final BooleanSetting rememberGui = new BooleanSetting("remember_gui", "Remember interface layout",
            "Remember module selection, category panel positions and folded state between sessions.", true);
    public final ColorSetting accentColor = new ColorSetting("accent_color", "Accent color",
            "Interface highlight color, including alpha.", 0xFF8B9DFF);
    public final DoubleSetting panelOpacity = new DoubleSetting("panel_opacity", "Menu panel opacity",
            "Opacity of NexVisuals panels over the live world.", .9, .35, 1);
    public final EnumSetting<ParticleBudget.Quality> particleQuality = new EnumSetting<>("particle_quality", "Particle quality",
            "Global cosmetic emission/live-particle/distance limits. Individual module settings are preserved; vanilla Minimal remains authoritative.",
            ParticleBudget.Quality.HIGH, ParticleBudget.Quality.class);
    private final List<Setting<?>> settings = List.of(animationDuration, rememberGui, accentColor, panelOpacity, particleQuality);
    private Category selectedCategory = Category.HUD;
    private String selectedModule = "";
    private final Map<Category,PanelPosition> panels = new EnumMap<>(Category.class);
    public Map<Category,PanelPosition> panelPositions() { return Collections.unmodifiableMap(panels); }
    public void setPanelPosition(Category category,PanelPosition position) { panels.put(Objects.requireNonNull(category),Objects.requireNonNull(position)); }
    public void clearPanelPositions() { panels.clear(); }

    public List<Setting<?>> settings() { return settings; }
    public Category selectedCategory() { return selectedCategory; }
    public void setSelectedCategory(Category category) { selectedCategory = Objects.requireNonNull(category); }
    public String selectedModule() { return selectedModule; }
    public void setSelectedModule(String id) { selectedModule = Objects.requireNonNullElse(id, ""); }

    public void reset() {
        settings.forEach(Setting::reset);
        selectedCategory = Category.HUD;
        selectedModule = "";
        panels.clear();
    }
}
