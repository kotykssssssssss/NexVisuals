package dev.nexvisuals.core.config;

/** Trusted bundled JSON recipes. User profile names never participate in this resource path. */
public enum BuiltinProfile {
    CLEAN("Clean", "Compact HUD, a cyan dot, small hands and quiet container feedback."),
    AURORA("Aurora", "Cyan/violet impacts, luminous trails, conical hat and coordinated interface."),
    CINEMATIC("Cinematic", "Warm sky, slower rings, smooth camera and a larger angled viewmodel.");
    private final String label, description;
    BuiltinProfile(String label, String description) { this.label = label; this.description = description; }
    public String label() { return label; }
    public String description() { return description; }
    String resource() { return "/nexvisuals/presets/" + name().toLowerCase(java.util.Locale.ROOT) + ".json"; }
}
