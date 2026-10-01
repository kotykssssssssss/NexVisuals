package dev.nexvisuals.core.animation;

/** Pure cosmetic poses sampled from vanilla's existing 40-tick activation progress. */
public final class TotemMotion {
    public enum Style { VANILLA, FLOAT, SPIN, POP }
    public record Pose(double y,double z,double pitch,double yaw,double roll,double scale) { }
    private TotemMotion() { }
    public static Pose sample(Style style,double progress,Easing easing,double motion,double turns) {
        double p=EffectMath.unit(progress);
        double appear=easing.apply(Math.min(1,p/.18)),leave=1-easing.apply(Math.max(0,(p-.78)/.22));
        double scale=appear*leave;
        return switch(style) {
            case VANILLA -> new Pose(0,0,0,0,0,1);
            case FLOAT -> new Pose(Math.sin(p*Math.PI)*.18*motion,Math.sin(p*Math.PI)*.22*motion,
                    8*Math.sin(p*Math.PI*2)*motion,0,6*Math.sin(p*Math.PI*2)*motion,scale);
            case SPIN -> new Pose(Math.sin(p*Math.PI)*.1*motion,0,-10*Math.sin(p*Math.PI)*motion,
                    360*turns*easing.apply(p),8*Math.sin(p*Math.PI)*motion,scale);
            case POP -> new Pose(0,Math.sin(p*Math.PI)*.35*motion,0,0,12*Math.sin(p*Math.PI*2)*motion,
                    scale*(1+.2*Math.sin(Math.min(1,p/.4)*Math.PI)*motion));
        };
    }
}
