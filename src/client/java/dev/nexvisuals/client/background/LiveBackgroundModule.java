package dev.nexvisuals.client.background;

import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.client.gui.NexVisualsScreen;
import dev.nexvisuals.client.gui.ProfilesScreen;
import dev.nexvisuals.core.animation.MenuClock;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;

/** Background only: no ownership of buttons, clicks, layout, world rendering or loading screens. */
public final class LiveBackgroundModule extends VisualModule {
    public enum Background { VANILLA, LIVE }
    public enum Style { AURORA, FLOW, NEBULA, WAVES, MINIMAL, NEXVISUALS }
    public final EnumSetting<Background> background=add(new EnumSetting<>("background","Background","Vanilla restores the ordinary panorama/world background without losing your colors.",Background.LIVE,Background.class));
    public final EnumSetting<Style> style=add(new EnumSetting<>("style","Live style","Six different procedural shapes. Presets also choose colors and motion; all remain editable.",Style.NEXVISUALS,Style.class));
    public final DoubleSetting speed=number("speed","Animation speed",.65,0,2);
    public final DoubleSetting intensity=number("intensity","Intensity",.7,0,1);
    public final DoubleSetting brightness=number("brightness","Brightness",.85,.3,1.4);
    public final DoubleSetting saturation=number("saturation","Saturation",1,0,1.5);
    public final ColorSetting primary=color("primary","Primary color",0xFF687ED4);
    public final ColorSetting secondary=color("secondary","Secondary color",0xFF235C75);
    public final ColorSetting accent=color("accent","Accent color",0xFFB9DBE8);
    public final DoubleSetting motion=number("motion","Motion amount",.65,0,1);
    public final DoubleSetting softness=number("softness","Shape softness",.55,.1,1);
    public final IntSetting particles=add(new IntSetting("particles","Subtle motes","Approximate particle count from a deterministic GPU grid; no particle objects/history.",10,0,32));
    public final DoubleSetting moteSize=number("mote_size","Mote size",1,.5,3);
    public final DoubleSetting moteOpacity=number("mote_opacity","Mote opacity",1,0,2);
    public final DoubleSetting moteSpeed=number("mote_speed","Mote drift speed",1,0,3);
    public final DoubleSetting moteTwinkle=number("mote_twinkle","Mote twinkle",1,0,1);
    public final BooleanSetting moteAccent=add(new BooleanSetting("mote_accent","Motes use accent","Match the wallpaper accent, or set an independent mote color.",true));
    public final ColorSetting moteColor=color("mote_color","Mote color",0xFFD1E5FF);
    public final DoubleSetting dim=number("dim","Background dim",.25,0,.8);
    public final BooleanSetting pauseMenu=add(new BooleanSetting("pause_menu","Also in pause menu","Optional opaque wallpaper behind vanilla pause buttons; containers/gameplay screens remain untouched.",false));
    private final MenuClock clock=new MenuClock();
    private final LiveBackgroundRenderer renderer=new LiveBackgroundRenderer(this);
    private Screen lastDrawn;
    private String failure="";

    public LiveBackgroundModule() {
        super("live_background","Live Background","Slow GPU wallpapers with editable colors. Title, NexVisuals and optional pause menu; compatible with Console Menu layout.",Category.INTERFACE);
        group("Background",background,style,pauseMenu,dim);
        group("Colors",primary,secondary,accent,brightness,saturation,intensity);
        group("Motion & shapes",speed,motion,softness);
        group("Motes",particles,moteSize,moteOpacity,moteSpeed,moteTwinkle,moteAccent,moteColor);
        for(var s:new Setting<?>[]{moteSize,moteOpacity,moteSpeed,moteTwinkle,moteAccent}) s.visibleWhen(()->particles.get()>0);
        moteColor.visibleWhen(()->particles.get()>0&&!moteAccent.get());
        preset("NexVisuals","Layered diagonal ribbons with violet/cyan depth and soft light edges.");
        preset("Aurora","Flowing horizontal light curtains in blue and mint.","style","AURORA","primary","#FF3D9B8D","secondary","#FF225685","accent","#FFABE5C2","speed",.5,"softness",.7,"particles",14);
        preset("Flow","Slow elliptical color fields, with no ribbons or specks.","style","FLOW","primary","#FF675396","secondary","#FF367F95","accent","#FFD5A5BF","softness",.85,"motion",.5,"particles",0);
        preset("Nebula","Soft cloudy violet depths with sparse drifting stars.","style","NEBULA","primary","#FF6E4B94","secondary","#FF273F6F","accent","#FFAABEEB","speed",.35,"brightness",.7,"softness",.8,"particles",22);
        preset("Waves","Broad curved green bands and fine light edges, inspired by calm dashboard wallpapers.","style","WAVES","primary","#FF529C58","secondary","#FF224B31","accent","#FFC5E38A","speed",.4,"motion",.45,"softness",.45,"particles",0);
        preset("Minimal","A restrained diagonal gradient with almost imperceptible movement.","style","MINIMAL","primary","#FF334660","secondary","#FF242D43","accent","#FF647A8E","speed",.2,"motion",.12,"intensity",.45,"particles",0);
        preset("Stardust","Quiet cloudy depths with larger, slowly drifting pale motes.","style","NEBULA","primary","#FF685589","secondary","#FF243A56","particles",24,"mote_size",1.5,"mote_opacity",1.35,"mote_speed",.5,"mote_twinkle",.5,"speed",.35,"dim",.32);
    }
    private DoubleSetting number(String id,String name,double value,double min,double max) {
        return add(new DoubleSetting(id,name,"Live preview behind the menu. Speed or motion = 0 freezes animation.",value,min,max));
    }
    private ColorSetting color(String id,String name,int value) {
        return add(new ColorSetting(id,name,"ARGB; alpha controls this layer's contribution. No external images are loaded.",value));
    }
    private boolean supports(Screen screen) {
        return screen instanceof TitleScreen || screen instanceof NexVisualsScreen || screen instanceof ProfilesScreen
                || screen instanceof dev.nexvisuals.client.gui.ColorPickerScreen
                || pauseMenu.get() && screen instanceof PauseScreen;
    }
    public boolean render(Screen screen,GuiGraphics graphics) {
        if(!enabled() || background.get()!=Background.LIVE || !failure.isEmpty() || !supports(screen)) return false;
        var mod=NexVisualsClient.instance();
        boolean reduced=mod!=null && mod.consoleMenu().enabled() && mod.consoleMenu().reduceMotion.get();
        double phase=clock.advance(speed.get(),motion.get()>0 && !reduced);
        try {
            renderer.render(Minecraft.getInstance(),graphics,phase,!(screen instanceof TitleScreen));
            lastDrawn=screen;
            return true;
        } catch(RuntimeException exception) {
            failure="Paused: live background GPU pass failed; Vanilla fallback. Toggle OFF/ON to retry.";
            NexVisualsClient.LOGGER.error("Live Background failed; restoring ordinary menu background",exception);
            closeRenderer();
            return false;
        }
    }
    public boolean usedOn(Screen screen) { return enabled() && lastDrawn==screen && failure.isEmpty() && background.get()==Background.LIVE; }
    /** No draw loop outside supported menus; stop the clock so returning never catches up. */
    public void tick(Minecraft client) {
        if(enabled() && (background.get()==Background.VANILLA || !supports(client.screen))) closeRenderer();
    }
    public void closeRenderer() { renderer.close(); lastDrawn=null; clock.advance(0,false); }
    @Override protected void onDisable() { closeRenderer(); failure=""; }
    @Override public String runtimeStatus() {
        if(!enabled() || background.get()==Background.VANILLA) return "Vanilla background; live colors remain saved.";
        return failure.isEmpty()?"Live in title/settings menus; speed or motion 0 freezes it.":failure;
    }
}
