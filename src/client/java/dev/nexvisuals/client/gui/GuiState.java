package dev.nexvisuals.client.gui;

import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.Setting;
import dev.nexvisuals.core.ui.PanelPosition;

import java.util.HashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Navigation and unfinished text survive a resize without being written to the config. */
final class GuiState {
    Category category;
    String query = "";
    String selectedId = "";
    boolean globalSettings;
    boolean narrowDetails;
    int moduleScroll;
    int settingScroll;
    int boardPage;
    private boolean rebuildRequested;
    final Map<Category, Integer> panelScroll = new EnumMap<>(Category.class);
    final Map<Category, PanelPosition> panelPositions = new EnumMap<>(Category.class);
    final List<Category> panelOrder = new java.util.ArrayList<>(List.of(Category.values()));
    final Map<Setting<?>, String> drafts = new HashMap<>();
    final Map<String, Integer> sections = new HashMap<>();
    final Set<Setting<?>> invalidDrafts = new HashSet<>();

    GuiState(GlobalSettings globals) {
        if (globals.rememberGui.get()) {
            category = globals.selectedCategory();
            selectedId = globals.selectedModule();
            panelPositions.putAll(globals.panelPositions());
        }
    }

    void requestRebuild() { rebuildRequested = true; }

    /** Input can arrive between ticks. Build the requested view before the next frame or event. */
    void rebuildIfRequested(Runnable rebuild) {
        if (!rebuildRequested) return;
        rebuildRequested = false;
        rebuild.run();
    }

    /** EditBox also notifies its responder on cursor moves, including focus restoration. */
    boolean updateQuery(String value) {
        if (query.equals(value)) return false;
        query = value;
        moduleScroll = 0;
        panelScroll.clear(); boardPage=0;
        return true;
    }

    List<VisualModule> matching(ModuleRegistry registry) {
        return matching(registry,category);
    }
    List<VisualModule> matching(ModuleRegistry registry,Category filter) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return registry.all().stream()
                .filter(module -> filter == null || module.category() == filter)
                .filter(module -> needle.isEmpty() || (module.name() + " " + module.description()
                        + " " + module.id()).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    void remember(GlobalSettings globals) {
        if (globals.rememberGui.get()) {
            if (category != null) globals.setSelectedCategory(category);
            globals.setSelectedModule(selectedId);
            globals.clearPanelPositions();
            panelPositions.forEach(globals::setPanelPosition);
        }
    }
}
