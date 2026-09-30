package dev.nexvisuals.core.animation;

/** Game-time transition, reusable for environment effects. Rewinds/world changes restart cleanly. */
public final class EnvironmentEnvelope {
    private double value, previousTime=Double.NaN;
    public double update(double target,double seconds,double responseSeconds) {
        double elapsed=Double.isNaN(previousTime)?0:seconds-previousTime;
        if(elapsed<0 || elapsed>1) { value=0;elapsed=0; }
        previousTime=seconds;
        value=Smoothing.approach(value,Math.clamp(target,0,1),elapsed*1000,responseSeconds*1000);
        if(value<.001 && target==0) value=0;
        return value;
    }
    public void reset() { value=0;previousTime=Double.NaN; }
}
