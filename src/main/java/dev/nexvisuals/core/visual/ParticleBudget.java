package dev.nexvisuals.core.visual;

/** One bounded admission gate. Quality changes density/caps, never stored module values. */
public final class ParticleBudget {
    public enum Quality {
        LOW(32,256,32,2), MEDIUM(80,512,48,1.5), HIGH(128,768,64,1), ULTRA(192,1024,96,1);
        public final int perTick, liveCap, distance;
        public final double stride;
        Quality(int perTick,int liveCap,int distance,double stride) {
            this.perTick=perTick;this.liveCap=liveCap;this.distance=distance;this.stride=stride;
        }
    }
    private Object level;
    private long tick=Long.MIN_VALUE;
    private int attempts,admitted;
    public boolean admit(Object world,long now,Quality quality,boolean decreased) {
        if(world!=level || now!=tick) { level=world;tick=now;attempts=admitted=0; }
        int limit=decreased?Math.min(48,quality.perTick):quality.perTick;
        // Thin a sequence uniformly instead of cutting off its end. No per-effect random flicker.
        boolean chosen=(int)(attempts/quality.stride)!=(int)((++attempts)/quality.stride);
        if(!chosen || admitted>=limit) return false;
        admitted++;
        return true;
    }
}
