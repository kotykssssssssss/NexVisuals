package dev.nexvisuals.client.gui.title;

import dev.nexvisuals.client.effect.ConsoleMenuModule;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.*;
import dev.nexvisuals.core.hud.ConsoleLayout;
import java.util.*;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Keeps the original buttons and actions, including disabled reasons, keyboard access and tooltips. */
public final class ConsoleTitleState {
    private static final Set<String> PRIMARY = Set.of("menu.singleplayer","menu.multiplayer","menu.online","menu.playdemo","menu.resetdemo");
    private final TitleScreen screen;
    private final ConsoleMenuModule theme;
    private final ConsoleLayout layout;
    private final List<Control> controls=new ArrayList<>();
    private final Map<Button,Control> skinned=new IdentityHashMap<>();
    private final String subtitle=Component.translatable("nexvisuals.menu.subtitle").getString();
    private final long opened=System.nanoTime();
    private record Control(AbstractWidget button, ConsoleLayout.Rect bounds, int row, Transition hover) { }

    public ConsoleTitleState(TitleScreen screen, ConsoleMenuModule theme, Runnable settings, Runnable vanilla) {
        this.screen=screen; this.theme=theme;
        List<AbstractWidget> buttons=Screens.getButtons(screen);
        List<AbstractWidget> primary=buttons.stream().filter(b->PRIMARY.contains(key(b)) || b.getMessage().getString().equals("Create Test World")).toList();
        layout=ConsoleLayout.of(screen.width,screen.height,theme.alignment.get(),Math.max(1,primary.size()));
        for(int i=0;i<primary.size();i++) add(primary.get(i),layout.primary(i),i);
        int left=layout.panel().x()+8, available=layout.panel().width()-16, utilityY=layout.utilityY();
        int small=(available-52)/2;
        for(AbstractWidget button:buttons) switch(key(button)) {
            case "options.language" -> add(button,new ConsoleLayout.Rect(left,utilityY,20,20),primary.size());
            case "menu.options" -> add(button,new ConsoleLayout.Rect(left+24,utilityY,small,20),primary.size());
            case "menu.quit" -> add(button,new ConsoleLayout.Rect(left+28+small,utilityY,small,20),primary.size());
            case "options.accessibility" -> add(button,new ConsoleLayout.Rect(left+available-20,utilityY,20,20),primary.size());
            default -> { }
        }
        Button appearance=Button.builder(Component.translatable("nexvisuals.menu.appearance"), b->settings.run()).build();
        Button disable=Button.builder(Component.translatable("nexvisuals.menu.vanilla"), b->vanilla.run()).build();
        buttons.add(appearance); buttons.add(disable);
        add(appearance,new ConsoleLayout.Rect(left,layout.customY(),available-72,20),primary.size()+1);
        add(disable,new ConsoleLayout.Rect(left+available-68,layout.customY(),68,20),primary.size()+1);
        updatePositions();
    }
    private static String key(AbstractWidget button) {
        return button.getMessage().getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
    }
    private void add(AbstractWidget button, ConsoleLayout.Rect bounds, int row) {
        Control control=new Control(button,bounds,row,new Transition(130,Easing.OUT_CUBIC));
        controls.add(control);
        if(button instanceof Button b) skinned.put(b,control);
        button.setWidth(bounds.width()); button.setHeight(bounds.height());
    }
    private void updatePositions() {
        double age=(System.nanoTime()-opened)/1_000_000.0;
        int duration=theme.reduceMotion.get()?0:theme.entrance.get();
        for(Control control:controls) {
            double progress=MenuMotion.entrance(age,control.row(),duration);
            int offset=(int)Math.round((1-progress)*18)*(theme.reverse.get()?-1:1);
            control.button().setPosition(control.bounds().x()+offset,control.bounds().y());
        }
    }
    public TitleScreen screen() { return screen; }
    public int logoOffsetX() { return layout.centerX()-screen.width/2; }
    public int logoOffsetY() { return layout.logoY()-30; }

    public void beforeWidgets(GuiGraphics g) {
        updatePositions();
        var mod=dev.nexvisuals.client.NexVisualsClient.instance();
        if(mod==null || !mod.liveBackground().usedOn(screen)) ConsoleTheme.backdrop(g,theme,false);
        var panel=layout.panel();
        ConsoleTheme.panel(g,panel.x(),panel.y(),panel.width(),panel.height(),theme);
        var font=Minecraft.getInstance().font;
        Draw.text(g,font,subtitle,layout.centerX()-font.width(subtitle)/2,panel.y()+11,theme.text.get(),true);
    }
    public boolean skin(Button button, GuiGraphics g) {
        Control control=skinned.get(button);
        if(control==null) return false;
        control.hover().setDurationMillis(theme.reduceMotion.get()?0:130);
        control.hover().target(button.active && button.isHoveredOrFocused());
        ConsoleTheme.button(g,button.getX(),button.getY(),button.getWidth(),button.getHeight(),control.hover().value(),button.getAlpha(),button.active,theme);
        var font=Minecraft.getInstance().font;
        String label=font.plainSubstrByWidth(button.getMessage().getString(),button.getWidth()-18);
        Draw.text(g,font,label,button.getX()+(button.getWidth()-font.width(label))/2,button.getY()+(button.getHeight()-font.lineHeight)/2,
                Draw.withAlpha(button.active?theme.text.get():0xFF979B92,button.getAlpha()),true);
        return true;
    }
}
