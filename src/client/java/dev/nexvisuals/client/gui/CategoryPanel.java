package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.ui.PanelPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import java.util.List;
import java.util.function.Consumer;

/** A movable, collapsible category window with an independent stable scrolling list. */
final class CategoryPanel {
    final Category category;
    final NexButton header;
    private final ScrollPane pane;
    private final List<VisualModule> modules;
    private final GlobalSettings globals;
    private final Consumer<PanelPosition> remember;
    private final Runnable rebuild;
    private final GuiLayout.Rect workspace;
    private final int width, fullHeight;
    private int x,y;
    private boolean collapsed;
    CategoryPanel(Category category,GuiLayout.Rect slot,GuiLayout.Rect workspace,PanelPosition saved,int scroll,
                  List<VisualModule> modules,GlobalSettings globals,boolean interactive,
                  Consumer<AbstractWidget> register,Consumer<String> open,Consumer<PanelPosition> remember,Runnable rebuild) {
        this.category=category; this.workspace=workspace; this.modules=modules; this.globals=globals;
        this.remember=remember; this.rebuild=rebuild; width=slot.width(); fullHeight=slot.height();
        x=saved==null?slot.x():workspace.x()+(int)Math.round(saved.x()*Math.max(0,workspace.width()-width));
        y=saved==null?slot.y():workspace.y()+(int)Math.round(saved.y()*Math.max(0,workspace.height()-fullHeight));
        collapsed=saved!=null && saved.collapsed();
        String label=category.displayName()+"  "+modules.size()+ (collapsed?" +":" -");
        header=new NexButton(x,y,width,24,()->label,()->false,this::toggle,globals);
        header.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.literal("Drag header to move. Click / right click to fold. Each list scrolls independently.")));
        header.active=interactive; register.accept(header);
        pane=new ScrollPane(body(),scroll);
        if(!collapsed) {
            int row=3;
            for(VisualModule module:modules) {
                ModuleRow button=new ModuleRow(x+4,width-10,module,globals,()->open.accept(module.id()));
                button.active=interactive; register.accept(button); pane.add(button,row); row+=24;
            }
        }
    }
    GuiLayout.Rect headerArea() { return new GuiLayout.Rect(x,y,width,24); }
    GuiLayout.Rect bounds() { return new GuiLayout.Rect(x,y,width,collapsed?24:fullHeight); }
    private GuiLayout.Rect body() { return new GuiLayout.Rect(x,y+24,width,Math.max(0,fullHeight-24)); }
    int scroll() { return pane.scroll(); }
    boolean contains(double mx,double my) { return bounds().contains(mx,my); }
    void scrollBy(int amount) { if(!collapsed) pane.scrollBy(amount); }
    void revealFocused() { if(!collapsed) pane.revealFocused(); }
    AbstractWidget click(MouseButtonEvent event,boolean twice) { return collapsed?null:pane.click(event,twice); }
    void move(int dx,int dy) {
        x=Math.clamp(x+dx,workspace.x(),Math.max(workspace.x(),workspace.right()-width));
        y=Math.clamp(y+dy,workspace.y(),Math.max(workspace.y(),workspace.bottom()-fullHeight));
        header.setX(x); header.setY(y); pane.moveTo(body()); savePosition();
    }
    void toggle() { collapsed=!collapsed; savePosition(); rebuild.run(); }
    private void savePosition() {
        remember.accept(new PanelPosition((double)(x-workspace.x())/Math.max(1,workspace.width()-width),
                (double)(y-workspace.y())/Math.max(1,workspace.height()-fullHeight),collapsed));
    }
    void render(GuiGraphics g,int mx,int my,float delta) {
        var r=bounds();
        Draw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),5,Draw.withAlpha(0xFF0C121F,globals.panelOpacity.get().floatValue()));
        Draw.border(g,r.x(),r.y(),r.width(),r.height(),1,Draw.withAlpha(globals.accentColor.get(),.32f));
        header.render(g,mx,my,delta);
        Draw.rect(g,x+5,y+23,width-10,1,Draw.withAlpha(globals.accentColor.get(),.5f));
        if(!collapsed) {
            pane.render(g,mx,my,delta);
            if(modules.isEmpty()) Draw.text(g,Minecraft.getInstance().font,"No modules",x+9,y+34,0xFF7C8BA6,false);
        }
    }
}
