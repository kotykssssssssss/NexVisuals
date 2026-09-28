package dev.nexvisuals.client.gui;

import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.Setting;

import java.util.HashMap;
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
    final Map<Setting<?>, String> drafts = new HashMap<>();
    final Set<Setting<?>> invalidDrafts = new HashSet<>();

    GuiState(GlobalSettings globals) {
        if (globals.rememberGui.get()) {
            category = globals.selectedCategory();
            selectedId = globals.selectedModule();
        }
    }

    List<VisualModule> matching(ModuleRegistry registry) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return registry.all().stream()
                .filter(module -> category == null || module.category() == category)
                .filter(module -> needle.isEmpty() || (module.name() + " " + module.description()
                        + " " + module.id()).toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    void remember(GlobalSettings globals) {
        if (globals.rememberGui.get()) {
            if (category != null) globals.setSelectedCategory(category);
            globals.setSelectedModule(selectedId);
        }
    }
}
