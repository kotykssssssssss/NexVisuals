package dev.nexvisuals.core.color;

import dev.nexvisuals.core.setting.ColorSetting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorPickerTest {
    private ColorSetting color(int argb) {return new ColorSetting("color","Color","",argb);}
    @Test void rgbHsvRoundTripPreservesEveryChannelIncludingAlpha() {
        int seed=0x12345678;
        for(int i=0;i<4096;i++) {
            seed=seed*1664525+1013904223;
            var hsv=HsvColor.fromArgb(seed);
            assertEquals(seed,HsvColor.argb(hsv.hue(),hsv.saturation(),hsv.value(),seed>>>24));
        }
        for(int argb:new int[]{0,0xFFFFFFFF,0x77808080,0xFFFF0000,0xFF00FF00,0xFF0000FF}) {
            var hsv=HsvColor.fromArgb(argb);assertEquals(argb,HsvColor.argb(hsv.hue(),hsv.saturation(),hsv.value(),argb>>>24));
        }
    }
    @Test void hueWrapsAndSaturationValueAndAlphaClamp() {
        assertEquals(0xFF00FF00,HsvColor.argb(1.0/3,2,4,999));
        assertEquals(HsvColor.argb(.7,.6,.8,123),HsvColor.argb(-.3,.6,.8,123));
        assertEquals(0,HsvColor.argb(4,-2,-3,-20));
        assertThrows(IllegalArgumentException.class,()->HsvColor.argb(Double.NaN,1,1,255));
        assertThrows(IllegalArgumentException.class,()->HsvColor.argb(0,Double.POSITIVE_INFINITY,1,255));
    }
    @Test void grayAndBlackRetainSelectedHueUntilSaturationIsRaised() {
        var setting=color(0xFF000000);var model=new ColorPickerModel(setting);
        model.hsv(.65,0,0);model.hsv(model.hue(),0,.8);assertEquals(.65,model.hue());
        model.hsv(model.hue(),.7,model.value());assertEquals(.65,model.hue());
        assertTrue(model.channel(0)>model.channel(16));
        setting.setHex("#66808080");model.sync();assertEquals(.65,model.hue());assertEquals(102,model.alpha());
    }
    @Test void channelAndAlphaEditingPreserveOtherChannelsAndSyncExternalHex() {
        var setting=color(0xAABBCCDD);var model=new ColorPickerModel(setting);
        model.channel(16,10);assertEquals(0xAA0ACCDD,setting.get());
        model.channel(8,900);model.alpha(50);assertEquals(0x320AFFDD,setting.get());
        setting.setHex("#DE55AA88");model.sync();assertEquals(0xDE55AA88,model.argb());
        model.hsv(model.hue(),model.saturation(),model.value());assertEquals(0xDE55AA88,setting.get());
        assertThrows(IllegalArgumentException.class,()->model.channel(24,5));
        assertThrows(IllegalArgumentException.class,()->model.hsv(Double.NaN,0,0));
        assertEquals(0xDE55AA88,setting.get());
        var restored=color(0);restored.fromJson(setting.toJson());assertEquals(setting.get(),restored.get());
    }
}
