package dev.nexvisuals.core.config;

import com.google.gson.JsonObject;
import dev.nexvisuals.core.module.ModuleRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Named local JSON snapshots. Profile IDs are deliberately filesystem-safe on Windows and Unix. */
public final class ProfileManager {
    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9_-]{0,47}");
    private static final Pattern DEVICE = Pattern.compile("con|prn|aux|nul|com[1-9]|lpt[1-9]");
    private final Path directory;
    private final ModuleRegistry registry;
    private final GlobalSettings globals;

    public ProfileManager(Path directory, ModuleRegistry registry, GlobalSettings globals) {
        this.directory = Objects.requireNonNull(directory).toAbsolutePath().normalize();
        this.registry = Objects.requireNonNull(registry);
        this.globals = Objects.requireNonNull(globals);
    }

    public Path directory() { return directory; }

    public static boolean validName(String name) {
        return name != null && NAME.matcher(name).matches() && !DEVICE.matcher(name).matches();
    }

    public List<String> list() throws IOException {
        if (!Files.exists(directory)) return List.of();
        ensureDirectory();
        try (var files = Files.list(directory)) {
            return files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5))
                    .filter(ProfileManager::validName).sorted().toList();
        }
    }

    /** Saving an existing ID intentionally updates that profile. */
    public void save(String name) throws IOException {
        Path profile = profilePath(name);
        new ConfigManager(profile, registry, globals).save();
    }

    /** An unreadable or corrupt profile leaves the currently active visual configuration intact. */
    public ConfigManager.LoadResult load(String name) throws IOException {
        Path profile = profilePath(name);
        if (!Files.isRegularFile(profile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Profile does not exist: " + name);
        }
        ConfigManager config = new ConfigManager(profile, registry, globals);
        JsonObject previous = config.snapshot();
        try {
            ConfigManager.LoadResult result = config.load();
            if (result.status() != ConfigManager.LoadStatus.LOADED) config.apply(previous);
            return result;
        } catch (IOException | RuntimeException exception) {
            config.apply(previous);
            throw exception;
        }
    }

    public boolean delete(String name) throws IOException {
        Path profile = profilePath(name);
        if (!Files.exists(profile, LinkOption.NOFOLLOW_LINKS)) return false;
        if (!Files.isRegularFile(profile, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Profile is not a regular file");
        return Files.deleteIfExists(profile);
    }

    public void restoreDefaults() {
        new ConfigManager(directory.resolve("defaults.json"), registry, globals).resetDefaults();
    }

    private Path profilePath(String name) throws IOException {
        if (!validName(name)) throw new IllegalArgumentException("Use 1–48 lowercase letters, digits, underscores or hyphens; avoid device names");
        ensureDirectory();
        Path path = directory.resolve(name + ".json").normalize();
        if (!path.getParent().equals(directory) || Files.isSymbolicLink(path)) {
            throw new IOException("Profile path must stay inside the profiles directory");
        }
        return path;
    }

    private void ensureDirectory() throws IOException {
        if (Files.isSymbolicLink(directory)) throw new IOException("Profiles directory cannot be a symbolic link");
        Files.createDirectories(directory);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid profiles directory");
    }
}
