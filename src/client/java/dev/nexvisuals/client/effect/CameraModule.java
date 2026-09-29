package dev.nexvisuals.client.effect;

import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.animation.Smoothing;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.DoubleSetting;
import dev.nexvisuals.core.setting.IntSetting;

/** Multiplies vanilla motion and adds a bounded, smoothly changing FOV offset. */
public final class CameraModule extends VisualModule {
    private final DoubleSetting fov = add(new DoubleSetting("fov_offset", "FOV offset", "Adds to vanilla FOV, clamped to its normal 30–110 range. No effect while scoping.", 0, -15, 15));
    private final DoubleSetting hurt = add(new DoubleSetting("hurt_intensity", "Hurt camera intensity", "Multiplier for vanilla damage tilt. Other damage feedback remains visible.", 1, 0, 1));
    private final DoubleSetting bob = add(new DoubleSetting("bobbing_intensity", "Bobbing intensity", "Multiplier for vanilla view bobbing. Respects the vanilla View Bobbing toggle.", 1, 0, 1));
    private final IntSetting smoothing = add(new IntSetting("smoothing", "FOV smoothing", "Response time in milliseconds; zero applies changes immediately.", 180, 0, 1000));
    private double currentFov;
    private long lastFrame;
    public CameraModule() { super("camera", "Camera Visuals", "Cosmetic FOV, damage tilt and view bobbing. No camera relocation.", Category.CAMERA); }
    public float bobScale() { return enabled() ? bob.get().floatValue() : 1; }
    public double hurtScale() { return enabled() ? hurt.get() : 1; }
    public float fov(float original) {
        if (!enabled()) return original;
        long now = System.nanoTime();
        double elapsed = lastFrame == 0 ? 0 : Math.max(0, (now - lastFrame) / 1_000_000.0);
        lastFrame = now;
        currentFov = Smoothing.approach(currentFov, fov.get(), elapsed, smoothing.get());
        return currentFov == 0 ? original : (float) Math.clamp(original + currentFov, 30, 110);
    }
    @Override protected void onEnable() { currentFov = 0; lastFrame = System.nanoTime(); }
    @Override protected void onDisable() { currentFov = 0; lastFrame = 0; }
}
