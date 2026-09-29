package dev.nexvisuals.core.module;

import com.google.gson.JsonElement;
import java.util.Map;

/** Declarative, local setting values; no executable preset files. */
public record ModulePreset(String name, String description, Map<String, JsonElement> values) {
    public ModulePreset { values = Map.copyOf(values); }
}
