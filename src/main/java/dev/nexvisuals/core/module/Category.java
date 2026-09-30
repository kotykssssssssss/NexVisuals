package dev.nexvisuals.core.module;

public enum Category {
    HUD("HUD"), COMBAT("Combat"), VIEWMODEL("Viewmodel"), PARTICLES("Particles"),
    CAMERA("Camera"), INTERFACE("Interface"), WORLD("World"), SHADERS("Post Processing"), GENERAL("General");

    private final String displayName;
    Category(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
}
