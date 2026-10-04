package dev.nexvisuals.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.Setting;
import dev.nexvisuals.core.ui.PanelPosition;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reads once at startup and writes explicitly on GUI close/client shutdown, never in rendering. */
public final class ConfigManager {
    public static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path path;
    private final ModuleRegistry registry;
    private final GlobalSettings globals;

    public enum LoadStatus { MISSING, LOADED, RECOVERED }
    public record LoadResult(LoadStatus status, List<String> warnings, Path backup) {
        public LoadResult { warnings = List.copyOf(warnings); }
    }

    public ConfigManager(Path path, ModuleRegistry registry, GlobalSettings globals) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
        this.registry = Objects.requireNonNull(registry);
        this.globals = Objects.requireNonNull(globals);
    }

    public Path path() { return path; }

    public LoadResult load() throws IOException {
        resetDefaults();
        if (!Files.exists(path)) return new LoadResult(LoadStatus.MISSING, List.of(), null);
        JsonObject root;
        try {
            JsonElement json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!json.isJsonObject()) throw new JsonParseException("Configuration root must be an object");
            root = json.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException | CharacterCodingException exception) {
            // Keep an exact backup before a subsequent save can replace the broken input.
            Path backup = backupCorruptFile();
            return new LoadResult(LoadStatus.RECOVERED,
                    List.of("Malformed configuration; defaults restored: " + exception.getMessage()), backup);
        }

        return new LoadResult(LoadStatus.LOADED, apply(root), null);
    }

    /** Apply the same schema used by the primary config and named visual profiles. */
    public List<String> apply(JsonObject root) {
        Objects.requireNonNull(root);
        resetDefaults();
        List<String> warnings = new ArrayList<>();
        if (root.has("schemaVersion")) {
            try {
                int version = root.get("schemaVersion").getAsInt();
                if (version != SCHEMA_VERSION) warnings.add("Unrecognized schema version " + version + "; loading known fields only");
            } catch (RuntimeException exception) {
                warnings.add("Invalid schemaVersion; loading known fields only");
            }
        }
        loadSettings(object(root, "global", warnings), globals.settings(), "global", warnings);
        JsonObject modules = object(root, "modules", warnings);
        if (modules != null) {
            for (VisualModule module : registry.all()) {
                JsonObject data = object(modules, module.id(), warnings);
                if (data == null) continue;
                loadSettings(object(data, "settings", warnings), module.settings(), module.id(), warnings);
                JsonElement enabled = data.get("enabled");
                if (enabled != null) {
                    if (enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
                        module.setEnabled(enabled.getAsBoolean());
                    } else warnings.add(module.id() + ".enabled: expected boolean; using default");
                }
            }
        }
        if (globals.rememberGui.get()) {
            JsonObject gui = object(root, "gui", warnings);
            if (gui != null) {
                try {
                    if (gui.has("category")) globals.setSelectedCategory(Category.valueOf(gui.get("category").getAsString()));
                } catch (RuntimeException exception) { warnings.add("gui.category: unknown category; using default"); }
                JsonElement selected = gui.get("module");
                if (selected != null && selected.isJsonPrimitive() && selected.getAsJsonPrimitive().isString()) {
                    registry.find(selected.getAsString()).ifPresent(module -> globals.setSelectedModule(module.id()));
                }
                JsonObject panels=object(gui,"panels",warnings);
                if(panels!=null) for(Category category:Category.values()) {
                    JsonObject panel=object(panels,category.name(),warnings);
                    if(panel==null) continue;
                    try {
                        var x=panel.get("x"); var y=panel.get("y"); var folded=panel.get("collapsed");
                        if(x==null || y==null || folded==null || !x.isJsonPrimitive() || !x.getAsJsonPrimitive().isNumber()
                                || !y.isJsonPrimitive() || !y.getAsJsonPrimitive().isNumber() || !folded.isJsonPrimitive() || !folded.getAsJsonPrimitive().isBoolean())
                            throw new IllegalArgumentException("Expected numeric x/y and boolean collapsed");
                        globals.setPanelPosition(category,new PanelPosition(x.getAsDouble(),y.getAsDouble(),folded.getAsBoolean()));
                    } catch(RuntimeException exception) { warnings.add("gui.panels."+category.name()+": invalid layout; using automatic placement"); }
                }
            }
        }
        return List.copyOf(warnings);
    }

    public void save() throws IOException {
        JsonObject root = snapshot();
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public JsonObject snapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.add("global", settingsJson(globals.settings()));
        JsonObject modules = new JsonObject();
        for (VisualModule module : registry.all()) {
            JsonObject data = new JsonObject();
            data.addProperty("enabled", module.enabled());
            data.add("settings", settingsJson(module.settings()));
            modules.add(module.id(), data);
        }
        root.add("modules", modules);
        if (globals.rememberGui.get()) {
            JsonObject gui = new JsonObject();
            gui.addProperty("category", globals.selectedCategory().name());
            gui.addProperty("module", globals.selectedModule());
            if(!globals.panelPositions().isEmpty()) {
                JsonObject panels=new JsonObject();
                globals.panelPositions().forEach((category,position)->{
                    JsonObject panel=new JsonObject(); panel.addProperty("x",position.x()); panel.addProperty("y",position.y()); panel.addProperty("collapsed",position.collapsed());
                    panels.add(category.name(),panel);
                });
                gui.add("panels",panels);
            }
            root.add("gui", gui);
        }

        return root;
    }

    public void resetDefaults() {
        globals.reset();
        for (VisualModule module : registry.all()) {
            module.setEnabled(false);
            module.settings().forEach(Setting::reset);
        }
    }

    private Path backupCorruptFile() throws IOException {
        String suffix = ".broken-" + Instant.now().toEpochMilli();
        Path backup = path.resolveSibling(path.getFileName() + suffix + ".json");
        int attempt = 0;
        while (Files.exists(backup)) backup = path.resolveSibling(path.getFileName() + suffix + "-" + (++attempt) + ".json");
        Files.copy(path, backup);
        return backup;
    }

    private static JsonObject object(JsonObject parent, String key, List<String> warnings) {
        JsonElement value = parent.get(key);
        if (value == null) return null;
        if (value.isJsonObject()) return value.getAsJsonObject();
        warnings.add(key + ": expected object; ignoring field");
        return null;
    }

    private static void loadSettings(JsonObject json, List<Setting<?>> settings, String owner, List<String> warnings) {
        if (json == null) return;
        for (Setting<?> setting : settings) {
            if (!json.has(setting.id())) continue;
            try {
                setting.fromJson(json.get(setting.id()));
            } catch (IllegalArgumentException | IllegalStateException | ArithmeticException exception) {
                setting.reset();
                warnings.add(owner + "." + setting.id() + ": invalid value; using default");
            }
        }
    }

    private static JsonObject settingsJson(List<Setting<?>> settings) {
        JsonObject result = new JsonObject();
        for (Setting<?> setting : settings) result.add(setting.id(), setting.toJson());
        return result;
    }
}
