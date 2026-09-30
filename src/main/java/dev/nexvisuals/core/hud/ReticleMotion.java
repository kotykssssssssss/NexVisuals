package dev.nexvisuals.core.hud;

/** Cosmetic envelopes only; callers supply their own movement/swing, never a target state. */
public final class ReticleMotion {
    private ReticleMotion() { }
    public static double breathe(double seconds,double frequency) { return .5+.5*Math.sin(seconds*frequency*Math.PI*2); }
    public static double movement(double horizontalVelocity) { return Math.clamp(Math.abs(horizontalVelocity)*4,0,1); }
    public static double swing(double progress) { return Math.sin(Math.clamp(progress,0,1)*Math.PI); }
    public static double scale(double envelope,double amplitude) { return 1+Math.clamp(envelope,0,1)*Math.clamp(amplitude,0,1); }
}
