package dev.nexvisuals.client.gui.title;

import dev.nexvisuals.client.effect.ConsoleMenuModule;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.EffectMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Pixel edges and bevels keep the Minecraft character without a new shader or texture pack. */
public final class ConsoleTheme {
    private ConsoleTheme() { }

    public static void backdrop(GuiGraphics g, ConsoleMenuModule theme, boolean loading) {
        int width=g.guiWidth(), height=g.guiHeight();
        Draw.rect(g,0,0,width,height,theme.skyTint.get());
        g.fillGradient(0,0,width,height,0x10000000,loading?0xD0101418:0x850C1014);
        double phase=theme.phase();
        if (theme.pixels.get()) for (int i=0;i<18;i++) {
            double travel=phase*(theme.reverse.get()?-1:1);
            int x=(int)wrap(i*73.31+travel*(2+i%3),width+16)-8;
            int y=(int)wrap(i*41.73-travel*(1+i%2),height+16)-8;
            int size=1+i%3;
            Draw.rect(g,x,y,size,size,Draw.withAlpha(theme.accent.get(),.07f+(i%4)*.025f));
        }
    }
    private static double wrap(double value, double bound) { return (value%bound+bound)%bound; }

    public static void panel(GuiGraphics g, int x, int y, int width, int height, ConsoleMenuModule theme) {
        Draw.rect(g,x+3,y+4,width,height,0x55000000);
        Draw.rect(g,x,y,width,height,theme.panel.get());
        Draw.border(g,x,y,width,height,2,0xC0161918);
        Draw.rect(g,x+2,y+2,width-4,1,0x606F7569);
        Draw.rect(g,x+2,y+3,1,height-5,0x406F7569);
        Draw.rect(g,x+4,y+4,width-8,2,Draw.withAlpha(theme.accent.get(),.7f));
        // Fixed, sparse flecks suggest stone; they never obscure the text or animate independently.
        for(int row=0;row<height/12;row++) for(int col=0;col<width/16;col++) {
            if((row*7+col*3)%5==0) Draw.rect(g,x+5+col*16,y+8+row*12,3,1,0x0BFFFFFF);
        }
    }
    public static void button(GuiGraphics g, int x, int y, int width, int height, double hover, float alpha,
                              boolean active, ConsoleMenuModule theme) {
        int neutral=EffectMath.color(theme.panel.get()|0xFF000000,0xFF898B80,.38);
        int face=EffectMath.color(neutral,theme.accent.get()|0xFF000000,active?hover*.28:0);
        if(!active) face=EffectMath.color(face,0xFF151817,.55);
        Draw.rect(g,x,y,width,height,Draw.withAlpha(0xFF151715,alpha));
        Draw.rect(g,x+1,y+1,width-2,height-2,Draw.withAlpha(face,alpha));
        Draw.rect(g,x+2,y+1,width-4,1,Draw.withAlpha(0xFFB1B3A7,alpha*.55f));
        Draw.rect(g,x+1,y+2,1,height-4,Draw.withAlpha(0xFFB1B3A7,alpha*.35f));
        Draw.rect(g,x+2,y+height-3,width-4,2,Draw.withAlpha(0xFF151715,alpha*.6f));
        if(active && hover>0) {
            Draw.border(g,x,y,width,height,1,Draw.withAlpha(theme.accent.get(),(float)hover*alpha));
            Draw.rect(g,x+4,y+height/2-2,2,4,Draw.withAlpha(theme.accent.get(),(float)hover*alpha));
        }
    }
    public static void loadingFooter(GuiGraphics g, ConsoleMenuModule theme) {
        int width=g.guiWidth(), height=g.guiHeight();
        int boxWidth=Math.min(300,width-20), x=(width-boxWidth)/2, y=height-55;
        panel(g,x,y,boxWidth,37,theme);
        var font=Minecraft.getInstance().font;
        Component tip=Component.translatable("nexvisuals.menu.loading_tip");
        String text=font.plainSubstrByWidth(tip.getString(),boxWidth-18);
        Draw.text(g,font,text,(width-font.width(text))/2,y+12,theme.text.get(),true);
        int step=theme.reduceMotion.get()?2:(int)(theme.phase()*2)%5;
        for(int i=0;i<5;i++) Draw.rect(g,width/2-18+i*8,y+26,5,3,
                Draw.withAlpha(theme.accent.get(),i==step?.9f:.25f));
    }
}
