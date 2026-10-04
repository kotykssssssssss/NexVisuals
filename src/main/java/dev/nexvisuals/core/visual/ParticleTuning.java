package dev.nexvisuals.core.visual;

/** Immutable per-style snapshot shared by its live particles until the next settings edit. */
public record ParticleTuning(ColorMode colorMode, double rainbowSpeed, double saturation, double rainbowBrightness,
        double hueOffset, double opacity, double brightness, double drag, double velocityScale, double gravityOffset,
        double scatter, double randomness, double sizeVariance, double lifetimeVariance, double velocityVariance,
        boolean customEnvelope, double fadeIn, double fadeOut, boolean customScale, double startSize, double endSize,
        double spin, double glow, double maxDistance, boolean distanceFade) {
    public enum ColorMode { LEGACY, STATIC, TWO_COLOR, GRADIENT, RAINBOW, THEME, RANDOM_BETWEEN }
    public static final ParticleTuning LEGACY = new ParticleTuning(ColorMode.LEGACY,.15,.65,1,0,1,1,.94,1,0,
            0,1,0,0,0,false,0,.35,false,1,.15,0,1,64,false);
}
