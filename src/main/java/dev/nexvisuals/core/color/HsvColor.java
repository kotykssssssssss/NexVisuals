package dev.nexvisuals.core.color;

/** HSV conversion independent of AWT/Minecraft; alpha is preserved explicitly. Hue is in turns. */
public record HsvColor(double hue,double saturation,double value) {
    public static HsvColor fromArgb(int argb) {
        double r=(argb>>>16&255)/255.0,g=(argb>>>8&255)/255.0,b=(argb&255)/255.0;
        double max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),delta=max-min,h=0;
        if(delta>0) {
            if(max==r) h=(g-b)/delta;
            else if(max==g) h=2+(b-r)/delta;
            else h=4+(r-g)/delta;
            h=h/6-Math.floor(h/6);
        }
        return new HsvColor(h,max==0?0:delta/max,max);
    }
    public static int argb(double hue,double saturation,double value,int alpha) {
        if(!Double.isFinite(hue)||!Double.isFinite(saturation)||!Double.isFinite(value)) throw new IllegalArgumentException("Finite HSV required");
        double h=(hue-Math.floor(hue))*6,s=Math.clamp(saturation,0,1),v=Math.clamp(value,0,1);
        double c=v*s,x=c*(1-Math.abs(h%2-1)),m=v-c,r=0,g=0,b=0;
        switch((int)h) {
            case 0 -> {r=c;g=x;} case 1 -> {r=x;g=c;} case 2 -> {g=c;b=x;}
            case 3 -> {g=x;b=c;} case 4 -> {r=x;b=c;} case 5 -> {r=c;b=x;}
        }
        return Math.clamp(alpha,0,255)<<24 | (int)Math.round((r+m)*255)<<16 | (int)Math.round((g+m)*255)<<8 | (int)Math.round((b+m)*255);
    }
}
