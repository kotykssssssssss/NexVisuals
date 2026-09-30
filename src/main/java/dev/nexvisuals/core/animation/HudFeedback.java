package dev.nexvisuals.core.animation;

/** Bounded visual envelopes from legitimate local state; no target or combat outcome inference. */
public final class HudFeedback {
    private HudFeedback() { }
    public static double lowHealth(double health,double maxHealth,double threshold) {
        if(!Double.isFinite(health)||!Double.isFinite(maxHealth)||!Double.isFinite(threshold)||maxHealth<=0||threshold<=0) return 0;
        return Math.clamp((threshold-Math.max(0,health)/maxHealth)/threshold,0,1);
    }
    public static double heartbeat(double seconds,double frequency) {
        if(!Double.isFinite(seconds)||!Double.isFinite(frequency)||frequency<=0) return 0;
        double phase=seconds*frequency-Math.floor(seconds*frequency);
        return Math.clamp(.18+.65*Math.exp(-Math.pow((phase-.18)/.085,2))+.4*Math.exp(-Math.pow((phase-.4)/.1,2)),0,1);
    }
    public static double durability(int damage,int maximum) {
        return maximum<=0?1:Math.clamp(((double)maximum-damage)/maximum,0,1);
    }
    public static String duration(int ticks,boolean infinite) {
        if(infinite) return "∞";
        int seconds=Math.max(0,ticks)/20;
        return seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;
    }
}
