package dev.nexvisuals.core.visual;

/** Pure, bounded sky calculations; time is the legitimate sun angle, never a modified world clock. */
public final class SkyMath {
    public record Weights(double day, double sunset, double night) { }
    private SkyMath() { }
    public static double smooth(double low, double high, double value) {
        double t = Math.clamp((value-low)/(high-low),0,1);
        return t*t*(3-2*t);
    }
    public static Weights weights(double sunAngle, double influence) {
        double elevation = Math.cos(sunAngle);
        double day = smooth(-.08,.30,elevation);
        double night = 1-smooth(-.30,.08,elevation);
        double sunset = Math.max(0,1-day-night);
        double amount = Math.clamp(influence,0,1);
        return new Weights(1-amount+day*amount,sunset*amount,night*amount);
    }
    public static int blend(int day, int sunset, int night, Weights weights) {
        int color=0xFF000000;
        for(int shift=0;shift<=16;shift+=8) {
            int component=(int)Math.round(((day>>>shift)&255)*weights.day()+((sunset>>>shift)&255)*weights.sunset()+((night>>>shift)&255)*weights.night());
            color |= Math.clamp(component,0,255)<<shift;
        }
        return color;
    }
    public static int grade(int color, double brightness, double saturation, int tint) {
        double r=(color>>>16)&255, g=(color>>>8)&255, b=color&255;
        double luma=r*.2126+g*.7152+b*.0722;
        int red=(int)Math.round((luma+(r-luma)*saturation)*brightness*((tint>>>16)&255)/255);
        int green=(int)Math.round((luma+(g-luma)*saturation)*brightness*((tint>>>8)&255)/255);
        int blue=(int)Math.round((luma+(b-luma)*saturation)*brightness*(tint&255)/255);
        return 0xFF000000|Math.clamp(red,0,255)<<16|Math.clamp(green,0,255)<<8|Math.clamp(blue,0,255);
    }
    /** Density may only add fog. It must never extend the vanilla visibility distance. */
    public static float fogDistance(float original, double density) { return (float)(original/Math.clamp(density,1,3)); }
}
