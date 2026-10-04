package dev.nexvisuals.client.interfacefx;

import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;

/** Shared settings for every ordinary AbstractContainerScreen. Slot logic remains vanilla. */
public final class ContainerVisualsModule extends VisualModule {
    public enum Opening { FADE, SCALE, SLIDE, NONE }
    public enum Motion { SLIDE, SMOOTH, ARC, POP, FADE }
    public enum ClickStyle { FRAME, DOUBLE_FRAME, DIAMOND }
    public final ColorSetting panel = add(new ColorSetting("panel", "Panel tint", "Translucent tint over the vanilla panel, below its items and labels.", 0x30283D62));
    public final ColorSetting accent = add(new ColorSetting("accent", "Accent", "Outline, hover and click feedback ARGB.", 0xB87EDCFF));
    public final BooleanSetting outline = add(new BooleanSetting("outline", "Panel outline", "Decorative frame around the ordinary container.", true));
    public final IntSetting rounding = add(new IntSetting("rounding", "Frame rounding", "Rounded cosmetic frame; vanilla textures and slots remain rectangular.", 5, 0, 12));
    public final EnumSetting<Opening> opening = add(new EnumSetting<>("opening", "Opening frame", "Animate only the decorative frame. Actual slots always stay aligned with the mouse.", Opening.SCALE, Opening.class));
    public final IntSetting duration = add(new IntSetting("duration", "Animation duration", "Milliseconds. Inventory actions are never delayed.", 180, 60, 450));
    public final EnumSetting<Easing> easing = add(new EnumSetting<>("easing", "Easing", "Opening, hover and transfer curve.", Easing.OUT_CUBIC, Easing.class));
    public final BooleanSetting hover = add(new BooleanSetting("hover", "Smooth slot hover", "Smoothly fade the last few hovered slots.", true));
    public final ColorSetting slotTint = add(new ColorSetting("slot_tint", "Slot background tint", "ARGB fill behind slot items. Transparent alpha disables it.", 0x1424314B));
    public final BooleanSetting slotOutline = add(new BooleanSetting("slot_outline", "Slot outlines", "Subtle individual outlines for active slots.", false));
    public final BooleanSetting clicks = add(new BooleanSetting("clicks", "Click reactions", "Short outline/pop for pickup, drop, right-click and quick craft.", true));
    public final BooleanSetting transfers = add(new BooleanSetting("transfers", "Quick-move animation", "Visual ghost only when the local predicted slot changes identify a complete transfer.", true));
    public final EnumSetting<Motion> motion = add(new EnumSetting<>("motion", "Transfer style", "Uncertain destinations fall back to source-slot feedback.", Motion.ARC, Motion.class));
    public final DoubleSetting scale = add(new DoubleSetting("scale", "Ghost scale", "Scale of the cosmetic item copy only.", 1, .5, 1.5));
    public final DoubleSetting intensity = add(new DoubleSetting("intensity", "Motion intensity", "Arc height and pop strength.", 1, .1, 2));
    public final BooleanSetting afterimage = add(new BooleanSetting("afterimage", "Ghost outline", "A fading accent outline around the moving icon.", false));
    public final EnumSetting<ClickStyle> clickStyle=add(new EnumSetting<>("click_style","Click shape","Original frame, paired expanding frames, or a small diamond ripple.",ClickStyle.FRAME,ClickStyle.class));
    public final DoubleSetting clickSpread=add(new DoubleSetting("click_spread","Click expansion","GUI pixels of cosmetic expansion, multiplied by motion intensity.",6,0,12));
    public final DoubleSetting clickOpacity=add(new DoubleSetting("click_opacity","Click opacity","Alpha multiplier; actual slot contents are unaffected.",1,0,1));
    public final IntSetting clickThickness=add(new IntSetting("click_thickness","Click thickness","Pixel outline thickness.",1,1,3));
    public final DoubleSetting clickDuration=add(new DoubleSetting("click_duration","Click lifetime multiplier","Independent lifetime for source-slot reactions; transfers keep Animation duration.",1,.5,2));
    public final BooleanSetting clickGradient=add(new BooleanSetting("click_gradient","Click color gradient","Interpolate from the container accent to the end color over life.",false));
    public final ColorSetting clickEnd=add(new ColorSetting("click_end","Click end color","ARGB end of the optional gradient.",0x608EABE0));
    public ContainerVisualsModule() {
        super("container_visuals", "Container Visuals", "Shared panel, hover, click and observed quick-move feedback. No inventory automation.", Category.INTERFACE);
        preset("Clean", "Subtle blue tint and fast slot reactions.", "opening", "FADE", "motion", "SMOOTH", "duration", 140);
        preset("Aurora", "A violet frame with arc transfers.", "panel", "#383D284F", "accent", "#C0B299FF", "opening", "SCALE", "motion", "ARC");
        preset("Snap", "Short sliding frame and pop reactions.", "opening", "SLIDE", "motion", "POP", "duration", 100);
        group("Panel",panel,accent,outline,rounding,opening,duration,easing);
        group("Slots",hover,slotTint,slotOutline);
        group("Click feedback",clicks,clickStyle,clickSpread,clickOpacity,clickThickness,clickDuration,clickGradient,clickEnd);
        group("Transfers",transfers,motion,scale,intensity,afterimage);
        for(var s:new Setting<?>[]{clickStyle,clickSpread,clickOpacity,clickThickness,clickDuration,clickGradient}) s.visibleWhen(clicks::get);
        clickEnd.visibleWhen(()->clicks.get()&&clickGradient.get());
        for(var s:new Setting<?>[]{motion,scale,intensity,afterimage}) s.visibleWhen(transfers::get);
        preset("Crystal Touch","Diamond click ripples and a quiet blue-to-violet fade.","click_style","DIAMOND","click_gradient",true,"click_duration",1.2,"opening","FADE","motion","SMOOTH");
        preset("Double Echo","Two short expanding slot frames; contents and hit areas remain vanilla.","click_style","DOUBLE_FRAME","click_spread",8,"duration",160,"click_opacity",.85);
    }
}
