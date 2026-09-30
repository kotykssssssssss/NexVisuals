package dev.nexvisuals.core.visual;

/** Reference color math for the lightweight GPU pass. Black remains black, including with gamma/contrast edits. */
public final class ColorGrade {
    public record Parameters(double brightness,double contrast,double saturation,double gamma,double temperature,double tint) { }
    private ColorGrade() { }
    public static double[] apply(double r,double g,double b,Parameters p) {
        double luminance=r*.2126+g*.7152+b*.0722;
        r=(luminance+(r-luminance)*p.saturation())*p.brightness()*(1+p.temperature()*.12+p.tint()*.06);
        g=(luminance+(g-luminance)*p.saturation())*p.brightness()*(1-p.tint()*.10);
        b=(luminance+(b-luminance)*p.saturation())*p.brightness()*(1-p.temperature()*.12+p.tint()*.06);
        return new double[]{channel(r,p),channel(g,p),channel(b,p)};
    }
    private static double channel(double color,Parameters p) {
        double c=Math.clamp(color,0,1), lo=Math.pow(c,p.contrast()), hi=Math.pow(1-c,p.contrast());
        return Math.pow(lo/Math.max(.00001,lo+hi),1/p.gamma());
    }
}
