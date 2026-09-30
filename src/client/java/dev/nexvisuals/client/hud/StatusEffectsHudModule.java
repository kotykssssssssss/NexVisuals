package dev.nexvisuals.client.hud;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.HudFeedback;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.*;

public final class StatusEffectsHudModule extends TextHudModule {
    private final IntSetting maximum=add(new IntSetting("max_rows","Maximum effects","Bound the custom list. Vanilla indicators remain visible separately.",6,1,12));
    private final BooleanSetting icons=add(new BooleanSetting("icons","Effect icons","Use vanilla/resource-pack icons.",true));
    private final BooleanSetting effectColors=add(new BooleanSetting("effect_colors","Effect colors","Tint text with each effect's own color, keeping configured text alpha.",false));
    private final BooleanSetting timers=add(new BooleanSetting("timers","Time remaining","Show duration already available to the local player.",true));
    private final BooleanSetting hideEmpty=add(new BooleanSetting("hide_empty","Hide when empty","Hide the panel when no local effects are active. The editor keeps a selectable panel.",true));
    private record Row(String label,Identifier icon,int color) { }
    private Row[] rows={};
    private int width=100,ticks;
    public StatusEffectsHudModule() {
        super("hud_status_effects","Status Effects HUD","Compact local effect names, level and duration. Complements existing vanilla icon styling without replacing it.",240,"Speed II  1:30");
        preset("Effect Cards","Icons with name, level and duration.");
        preset("Colored Effects","Effect-colored text beside icons.","effect_colors",true);
        preset("Minimal Timers","A short text-only list.","icons",false,"max_rows",4,"background",false);
    }
    @Override public void tick(Minecraft client) {
        if(!canDisplay(client)) {text="";rows=new Row[0];return;}
        if(!text.isEmpty()&&++ticks%5!=0) return;
        var effects=new ArrayList<MobEffectInstance>();
        for(var effect:client.player.getActiveEffects()) if(effect.showIcon()) effects.add(effect);
        effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        var list=new ArrayList<Row>();width=0;
        for(int i=0;i<Math.min(maximum.get(),effects.size());i++) {
            var instance=effects.get(i);var effect=instance.getEffect();
            String label=effect.value().getDisplayName().getString()+(instance.getAmplifier()>0?" "+(instance.getAmplifier()+1):"")
                    +(timers.get()?"  "+HudFeedback.duration(instance.getDuration(),instance.isInfiniteDuration()):"");
            list.add(new Row(label,Gui.getMobEffectSprite(effect),effect.value().getColor()));width=Math.max(width,client.font.width(label));
        }
        rows=list.toArray(Row[]::new);text=rows.length==0?"No effects":"Effects";
    }
    @Override protected int contentWidth(Minecraft client) { return rows.length==0?client.font.width("No effects"):width+(icons.get()?22:0); }
    @Override public boolean renderHud(Minecraft client,GuiGraphics g,DeltaTracker delta) {
        if(text.isEmpty() && canDisplay(client)) tick(client);
        if(hideEmpty.get() && rows.length==0) return false;
        return super.renderHud(client,g,delta);
    }
    @Override protected int contentHeight(Minecraft client) { return Math.max(1,rows.length)*(icons.get()?21:12); }
    @Override protected void drawContent(Minecraft client,GuiGraphics g,int color,boolean shadow) {
        if(rows.length==0) {Draw.text(g,client.font,"No effects",5,4,color,shadow);return;}
        for(int i=0;i<rows.length;i++) {
            int y=4+i*(icons.get()?21:12);
            if(icons.get()) g.blitSprite(RenderPipelines.GUI_TEXTURED,rows[i].icon,5,y,18,18);
            int tint=effectColors.get()?(color&0xFF000000)|(rows[i].color&0xFFFFFF):color;
            Draw.text(g,client.font,rows[i].label,icons.get()?27:5,y+(icons.get()?5:0),tint,shadow);
        }
    }
}
