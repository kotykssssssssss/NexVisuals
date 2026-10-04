package dev.nexvisuals.core.visual;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.animation.EffectMath;
import dev.nexvisuals.core.color.HsvColor;

/** Small deterministic helpers: no particle objects, world queries or graphics dependencies. */
public final class ParticleMath {
    private ParticleMath() {}
    public static double variance(double value, double amount, double sample) {
        return value * (1 + EffectMath.unit(amount) * (2 * EffectMath.unit(sample) - 1));
    }
    public static double envelope(double progress, double fadeIn, double fadeOut, Easing easing) {
        double p=EffectMath.unit(progress);
        double in=fadeIn<=0?1:easing.apply(EffectMath.unit(p/fadeIn));
        double out=fadeOut<=0?1:1-easing.apply(EffectMath.unit((p-(1-fadeOut))/fadeOut));
        return Math.min(in,out);
    }
    public static double distanceFade(double distanceSquared, double maximum, boolean fade) {
        if(!Double.isFinite(distanceSquared) || maximum<=0 || distanceSquared>=maximum*maximum) return 0;
        if(!fade || distanceSquared<=maximum*maximum*.49) return 1;
        return 1-EffectMath.unit((Math.sqrt(distanceSquared)/maximum-.7)/.3);
    }
    public static int color(ParticleTuning t,int start,int end,double life,double sample,double seconds,int accent) {
        int color=switch(t.colorMode()) {
            case LEGACY, GRADIENT -> EffectMath.color(start,end,life);
            case STATIC -> start;
            case TWO_COLOR -> sample<.5?start:end;
            case RANDOM_BETWEEN -> EffectMath.color(start,end,sample);
            case THEME -> (start&0xFF000000) | (accent&0xFFFFFF);
            case RAINBOW -> HsvColor.argb(seconds*t.rainbowSpeed()+t.hueOffset(),t.saturation(),t.rainbowBrightness(),start>>>24);
        };
        int out=Math.clamp((int)Math.round((color>>>24)*t.opacity()),0,255)<<24;
        for(int shift=0;shift<=16;shift+=8) out|=Math.clamp((int)Math.round((color>>>shift&255)*t.brightness()),0,255)<<shift;
        return out;
    }
    public static int light(int vanilla,double glow) {
        double amount=EffectMath.unit(glow);
        int block=vanilla&0xFFFF,sky=vanilla>>>16&0xFFFF;
        return (int)Math.round(block+(240-block)*amount) | (int)Math.round(sky+(240-sky)*amount)<<16;
    }
}
