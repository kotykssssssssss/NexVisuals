package dev.nexvisuals.client.hud;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.hud.Anchor;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.*;

/** Cached active names, not another module registry or a per-frame stream/sort. */
public final class ActiveModulesHudModule extends TextHudModule {
    public enum Order { ALPHABETICAL, WIDTH }
    private final BooleanSetting includeHud=add(new BooleanSetting("include_hud","Include HUD modules","Include other HUD elements in the active list.",false));
    private final BooleanSetting recipes=add(new BooleanSetting("show_presets","Show preset name","Append the matching module preset, or Custom.",false));
    private final BooleanSetting accent=add(new BooleanSetting("accent_line","Accent line","Draw a small colored line beside each row.",true));
    private final BooleanSetting glow=add(new BooleanSetting("glow","Soft accent halo","A small translucent UI backplate around the accent, not entity glow.",false));
    private final IntSetting maximum=add(new IntSetting("max_rows","Maximum rows","Keep the list bounded; append the remaining count.",12,3,24));
    private final EnumSetting<Order> order=add(new EnumSetting<>("sort","Sort","Alphabetically or longest labels first.",Order.WIDTH,Order.class));
    private final ModuleRegistry registry;
    private String[] rows={"Active visuals"};
    private int width=80,ticks;
    public ActiveModulesHudModule(ModuleRegistry registry) {
        super("hud_active_modules","Active Visuals","A compact list of enabled NexVisuals modules; excludes itself. No game/entity information.",8,"Active visuals",Anchor.TOP_RIGHT);
        this.registry=registry;
        preset("Clean List","Longest names first with a subtle accent.","anchor","TOP_RIGHT");
        preset("Detailed","Include preset labels and a small accent halo.","anchor","TOP_RIGHT","show_presets",true,"glow",true,"max_rows",8);
        preset("Minimal","Short alphabetical list without accents.","anchor","TOP_RIGHT","sort","ALPHABETICAL","accent_line",false,"background",false,"max_rows",6);
    }
    @Override public void tick(Minecraft client) {
        if(!canDisplay(client)) {text="";return;}
        if(!text.isEmpty()&&++ticks%4!=0) return;
        var labels=new ArrayList<String>();
        for(var m:registry.all()) if(m!=this && m.enabled() && (includeHud.get()||m.category()!=Category.HUD))
            labels.add(m.name()+(recipes.get()&&!m.presets().isEmpty()?" / "+m.currentPresetName():""));
        if(order.get()==Order.WIDTH) labels.sort(Comparator.<String>comparingInt(client.font::width).reversed());
        else labels.sort(String.CASE_INSENSITIVE_ORDER);
        int extra=Math.max(0,labels.size()-maximum.get());
        if(extra>0) {labels.subList(maximum.get(),labels.size()).clear();labels.add("+ "+extra+" more");}
        if(labels.isEmpty()) labels.add("No active visuals");
        rows=labels.toArray(String[]::new);width=0;for(String row:rows) width=Math.max(width,client.font.width(row));text="Active visuals";
    }
    @Override protected int contentWidth(Minecraft client) { return width+(accent.get()?6:0); }
    @Override protected int contentHeight(Minecraft client) { return rows.length*12; }
    @Override protected void drawContent(Minecraft client,GuiGraphics g,int color,boolean shadow) {
        for(int i=0;i<rows.length;i++) {
            int y=4+i*12;
            if(accent.get()) {
                if(glow.get()) Draw.roundedRect(g,3,y-1,6,11,2,Draw.withAlpha(color,.16f));
                Draw.rect(g,5,y+1,2,7,color);
            }
            Draw.text(g,client.font,rows[i],accent.get()?11:5,y,color,shadow);
        }
    }
}
