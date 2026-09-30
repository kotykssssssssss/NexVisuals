package dev.nexvisuals.core.color;

import dev.nexvisuals.core.setting.ColorSetting;

/** Retains hue on black/grey so selecting hue before saturation does not jump back to red. */
public final class ColorPickerModel {
    private final ColorSetting setting;
    private double hue,saturation,value;
    private int observed;
    public ColorPickerModel(ColorSetting setting) { this.setting=setting;observed=~setting.get();sync(); }
    public void sync() {
        if(observed==setting.get()) return;
        var hsv=HsvColor.fromArgb(setting.get());
        if(hsv.saturation()>0 && hsv.value()>0) hue=hsv.hue();
        saturation=hsv.saturation();value=hsv.value();observed=setting.get();
    }
    public double hue() { return hue; }
    public double saturation() { return saturation; }
    public double value() { return value; }
    public int alpha() { return setting.get()>>>24; }
    public int argb() { return setting.get(); }
    public void hsv(double h,double s,double v) {
        setting.set(HsvColor.argb(h,s,v,alpha()));
        hue=h-Math.floor(h);saturation=Math.clamp(s,0,1);value=Math.clamp(v,0,1);observed=setting.get();
    }
    public void alpha(int value) { setting.set((setting.get()&0xFFFFFF)|(Math.clamp(value,0,255)<<24));observed=setting.get(); }
    public int channel(int shift) { return setting.get()>>>shift&255; }
    public void channel(int shift,int value) {
        if(shift!=0 && shift!=8 && shift!=16) throw new IllegalArgumentException("RGB channel shift");
        setting.set(setting.get()&~(255<<shift) | Math.clamp(value,0,255)<<shift);sync();
    }
}
