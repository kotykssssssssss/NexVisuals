package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.config.GlobalSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A screen-local modal list: input cannot fall through to sliders or the search behind it. */
final class ChoicePopup {
    record Option(String label,String description) { }
    @FunctionalInterface interface Selector {
        void open(AbstractWidget anchor,List<Option> options,int selected,IntConsumer choose);
    }
    private final ScrollPane pane;
    private final List<NexButton> buttons=new ArrayList<>();
    private final Runnable close;
    private final GlobalSettings globals;
    private int focused;

    ChoicePopup(AbstractWidget anchor,int width,int height,List<Option> options,int selected,
                IntConsumer choose,Runnable close,GlobalSettings globals) {
        this.close=close;this.globals=globals;
        int w=Math.min(Math.max(anchor.getWidth(),140),Math.max(30,width-12));
        int h=Math.min(Math.min(8,options.size())*24+8,Math.max(30,height-12));
        int x=Math.clamp(anchor.getX(),6,Math.max(6,width-w-6));
        int y=anchor.getBottom()+2;
        if(y+h>height-6) y=anchor.getY()-h-2;
        y=Math.clamp(y,6,Math.max(6,height-h-6));
        pane=new ScrollPane(new GuiLayout.Rect(x,y,w,h),0);
        for(int i=0;i<options.size();i++) {
            int index=i;Option option=options.get(i);
            NexButton button=new NexButton(x+4,0,w-10,20,option::label,()->index==selected,()->choose.accept(index),globals);
            if(!option.description().isBlank()) button.setTooltip(Tooltip.create(Component.literal(option.description())));
            buttons.add(button);pane.add(button,4+i*24);
        }
        focus(Math.clamp(selected,0,buttons.size()-1));
    }
    private void focus(int index) {
        buttons.forEach(button->button.setFocused(false));
        focused=index;buttons.get(focused).setFocused(true);pane.revealFocused();
    }
    void render(GuiGraphics graphics,int mouseX,int mouseY,float delta) {
        graphics.nextStratum();
        var r=pane.area;
        Draw.roundedRect(graphics,r.x()-2,r.y()-2,r.width()+4,r.height()+4,4,0xFF101725);
        Draw.border(graphics,r.x()-2,r.y()-2,r.width()+4,r.height()+4,1,globals.accentColor.get());
        pane.render(graphics,mouseX,mouseY,delta);
    }
    boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(!pane.area.contains(event.x(),event.y())) {close.run();return true;}
        for(NexButton button:buttons) if(button.mouseClicked(event,doubleClick)) break;
        return true;
    }
    boolean scroll(double vertical) {pane.scrollBy((int)(-vertical*24));return true;}
    boolean keyPressed(KeyEvent event) {
        switch(event.key()) {
            case GLFW.GLFW_KEY_ESCAPE -> close.run();
            case GLFW.GLFW_KEY_UP -> focus(Math.max(0,focused-1));
            case GLFW.GLFW_KEY_DOWN -> focus(Math.min(buttons.size()-1,focused+1));
            case GLFW.GLFW_KEY_TAB -> focus(Math.clamp(focused+((event.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0?-1:1),0,buttons.size()-1));
            case GLFW.GLFW_KEY_PAGE_UP -> focus(Math.max(0,focused-8));
            case GLFW.GLFW_KEY_PAGE_DOWN -> focus(Math.min(buttons.size()-1,focused+8));
            case GLFW.GLFW_KEY_HOME -> focus(0);
            case GLFW.GLFW_KEY_END -> focus(buttons.size()-1);
            default -> buttons.get(focused).keyPressed(event);
        }
        return true;
    }
}
