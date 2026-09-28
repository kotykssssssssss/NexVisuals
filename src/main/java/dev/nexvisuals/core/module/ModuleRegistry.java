package dev.nexvisuals.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Registration order is also display order; the client builds this once during startup. */
public final class ModuleRegistry {
    private final Map<String, VisualModule> modules = new LinkedHashMap<>();
    private final List<VisualModule> orderedModules = new ArrayList<>();
    private final List<VisualModule> modulesView = Collections.unmodifiableList(orderedModules);

    public <M extends VisualModule> M register(M module) {
        Objects.requireNonNull(module);
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalArgumentException("Duplicate module ID: " + module.id());
        }
        orderedModules.add(module);
        return module;
    }

    public List<VisualModule> all() { return modulesView; }
    public Optional<VisualModule> find(String id) { return Optional.ofNullable(modules.get(id)); }
}
