package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.animation.Transition;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.VisualModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Compact toggle + settings affordance, retaining native focus, keyboard and narration behavior. */
final class ModuleRow extends Button {
    private final VisualModule module;
    private final GlobalSettings globals;
    private final Runnable settings;
    private final Transition hover = new Transition(160, Easing.OUT_CUBIC);
    private GuiLayout.Rect clip;
    ModuleRow(int x,int width,VisualModule module,GlobalSettings globals,Runnable settings) {
        super(x,0,width,22,Component.literal(module.name()),ignored->module.toggle(),DEFAULT_NARRATION);
        this.module=module; this.globals=globals; this.settings=settings;
        setTooltip(Tooltip.create(Component.literal(module.description()+" Left click: toggle. Right click / > / Right arrow: settings.")));
    }
    void clipped(GuiLayout.Rect area) { clip=area; }
    @Override public boolean isMouseOver(double x,double y) { return (clip==null || clip.contains(x,y)) && super.isMouseOver(x,y); }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        if(!active || !visible || !isMouseOver(e.x(),e.y())) return false;
        if(e.button()==1 || e.button()==0 && e.x()>=getRight()-20) { settings.run(); return true; }
        return super.mouseClicked(e,twice);
    }
    @Override public boolean keyPressed(KeyEvent e) {
        if(active && isFocused() && e.key()==GLFW.GLFW_KEY_RIGHT) { settings.run(); return true; }
        return super.keyPressed(e);
    }
    @Override protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
        return Component.literal(module.name()+(module.enabled()?", enabled.":", disabled.")+" Right arrow opens settings.");
    }
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta) {
        hover.setDurationMillis(globals.animationDuration.get());
        hover.target(isHoveredOrFocused());
        int shade=(int)(hover.value()*12);
        int base=0xFF000000 | ((18+shade)<<16) | ((24+shade)<<8) | (36+shade);
        Draw.roundedRect(g,getX(),getY(),width,height,3,Draw.withAlpha(base,globals.panelOpacity.get().floatValue()));
        if(module.enabled()) {
            Draw.roundedRect(g,getX()+4,getY()+7,7,7,2,globals.accentColor.get());
            Draw.rect(g,getX()+6,getY()+9,3,3,0xFFF4F7FF);
        } else Draw.border(g,getX()+4,getY()+7,7,7,1,0xFF5B6980);
        var font=Minecraft.getInstance().font;
        String text=font.plainSubstrByWidth(module.name(),Math.max(1,width-39));
        Draw.text(g,font,text,getX()+16,getY()+7,module.enabled()?0xFFF1F5FF:0xFFA7B2C6,false);
        Draw.text(g,font,">",getRight()-13,getY()+7,globals.accentColor.get(),false);
        if(isFocused()) Draw.border(g,getX(),getY(),width,height,1,globals.accentColor.get());
    }
}
