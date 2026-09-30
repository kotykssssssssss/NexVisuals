package dev.nexvisuals.client.gui;

import dev.nexvisuals.client.NexVisualsClient;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.color.ColorPickerModel;
import dev.nexvisuals.core.color.HsvColor;
import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.setting.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** A generic editor for an existing ColorSetting. Changes are live; Cancel restores the entry color. */
public final class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final ColorSetting setting;
    private final GlobalSettings globals;
    private final Runnable changed;
    private final ColorPickerModel model;
    private final int original;
    private boolean rgb, syncing;
    private int left, top, panelWidth, panelHeight, lastColor, controlHeight;
    private EditBox hex;

    public ColorPickerScreen(Screen parent, ColorSetting setting, GlobalSettings globals, Runnable changed) {
        super(Component.literal("Color: " + setting.name()));
        this.parent=parent;this.setting=setting;this.globals=globals;this.changed=changed;
        original=setting.get();model=new ColorPickerModel(setting);
    }
    @Override protected void init() {
        panelWidth=Math.min(540,width-16);panelHeight=Math.min(280,height-16);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        int bodyY=top+40, bodyHeight=panelHeight-76, padWidth=(panelWidth-38)/2;
        int rowStep=Math.min(24,Math.max(14,bodyHeight/6));
        controlHeight=Math.min(20,rowStep-2);
        addRenderableWidget(new ColorPad(left+12,bodyY,padWidth,bodyHeight-40,0));
        addRenderableWidget(new ColorPad(left+12,bodyY+bodyHeight-33,padWidth,12,1));
        addRenderableWidget(new ColorPad(left+12,bodyY+bodyHeight-16,padWidth,12,2));
        int right=left+panelWidth/2+8, controlWidth=panelWidth/2-20;
        addRenderableWidget(new NexButton(right,bodyY,controlWidth,Math.min(18,controlHeight),()->rgb?"RGB channels":"HSV channels",()->false,
                ()->{rgb=!rgb;rebuildWidgets();},globals));
        if(rgb) {
            addChannel(right,bodyY+rowStep,controlWidth,"Red",()->model.channel(16)/255.0,v->model.channel(16,(int)Math.round(v*255)),255);
            addChannel(right,bodyY+rowStep*2,controlWidth,"Green",()->model.channel(8)/255.0,v->model.channel(8,(int)Math.round(v*255)),255);
            addChannel(right,bodyY+rowStep*3,controlWidth,"Blue",()->model.channel(0)/255.0,v->model.channel(0,(int)Math.round(v*255)),255);
        } else {
            addChannel(right,bodyY+rowStep,controlWidth,"Hue",model::hue,v->model.hsv(v,model.saturation(),model.value()),360);
            addChannel(right,bodyY+rowStep*2,controlWidth,"Saturation",model::saturation,v->model.hsv(model.hue(),v,model.value()),100);
            addChannel(right,bodyY+rowStep*3,controlWidth,"Value",model::value,v->model.hsv(model.hue(),model.saturation(),v),100);
        }
        addChannel(right,bodyY+rowStep*4,controlWidth,"Alpha",()->model.alpha()/255.0,v->model.alpha((int)Math.round(v*255)),255);
        hex=new EditBox(font,right,bodyY+rowStep*5,controlWidth,Math.min(18,controlHeight),Component.literal("ARGB hexadecimal color"));
        hex.setMaxLength(9);hex.setValue(setting.hex());lastColor=setting.get();
        hex.setResponder(value->{
            if(syncing) return;
            try {setting.setHex(value);model.sync();lastColor=setting.get();hex.setTextColor(0xFFE9EDF7);}
            catch(IllegalArgumentException exception) {hex.setTextColor(0xFFFF939F);}
        });
        addRenderableWidget(hex);
        int buttonWidth=(panelWidth-32)/3,y=top+panelHeight-27;
        addRenderableWidget(new NexButton(left+12,y,buttonWidth,19,()->"Reset",()->false,
                ()->{setting.reset();model.sync();refreshHex();},globals));
        addRenderableWidget(new NexButton(left+16+buttonWidth,y,buttonWidth,19,()->"Cancel",()->false,
                ()->{setting.set(original);finish();},globals));
        addRenderableWidget(new NexButton(left+20+buttonWidth*2,y,buttonWidth,19,()->"Done",()->true,this::finish,globals));
    }
    private void addChannel(int x,int y,int w,String name,DoubleSupplier getter,DoubleConsumer setter,int units) {
        addRenderableWidget(new ChannelSlider(x,y,w,name,getter,setter,units));
    }
    private void refreshHex() {
        syncing=true;
        try {hex.setValue(setting.hex());hex.setTextColor(0xFFE9EDF7);lastColor=setting.get();}
        finally {syncing=false;}
    }
    private void finish() {changed.run();minecraft.setScreen(parent);}
    @Override public void onClose() {finish();}
    @Override public boolean isPauseScreen() {return false;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        var mod=NexVisualsClient.instance();
        boolean live=mod!=null && mod.liveBackground().render(this,g);
        Draw.rect(g,0,0,width,height,live?0x280A0E18:0xB80A0E18);
        Draw.roundedRect(g,left,top,panelWidth,panelHeight,8,Draw.withAlpha(0xFF101725,globals.panelOpacity.get().floatValue()));
        Draw.border(g,left,top,panelWidth,panelHeight,1,globals.accentColor.get());
        Draw.text(g,font,font.plainSubstrByWidth(setting.name(),panelWidth-94),left+12,top+12,0xFFF2F5FF,false);
        checker(g,left+panelWidth-65,top+10,50,16);
        Draw.rect(g,left+panelWidth-65,top+10,50,16,setting.get());
        model.sync();
        if(lastColor!=setting.get()) refreshHex();
        super.render(g,mouseX,mouseY,delta);
    }
    private static void checker(GuiGraphics g,int x,int y,int w,int h) {
        for(int j=0;j<h;j+=6) for(int i=0;i<w;i+=6)
            Draw.rect(g,x+i,y+j,Math.min(6,w-i),Math.min(6,h-j),((i/6+j/6)&1)==0?0xFF667080:0xFF303745);
    }

    /** SV area and hue/alpha strips use normal batched GUI quads, without textures or framebuffers. */
    private final class ColorPad extends AbstractWidget {
        private final int kind;
        private final int[] colors=new int[32];
        private double cachedHue=Double.NaN;
        private int cachedRgb;
        ColorPad(int x,int y,int width,int height,int kind) {
            super(x,y,width,Math.max(12,height),Component.literal(kind==0?"Saturation and value":kind==1?"Hue":"Opacity"));this.kind=kind;
        }
        private void select(double x,double y) {
            double horizontal=Math.clamp((x-getX())/Math.max(1,width-1),0,1);
            if(kind==0) model.hsv(model.hue(),horizontal,1-Math.clamp((y-getY())/Math.max(1,height-1),0,1));
            else if(kind==1) model.hsv(horizontal,model.saturation(),model.value());
            else model.alpha((int)Math.round(horizontal*255));
        }
        @Override public void onClick(MouseButtonEvent event,boolean doubleClick) {select(event.x(),event.y());}
        @Override protected void onDrag(MouseButtonEvent event,double dx,double dy) {select(event.x(),event.y());}
        @Override public boolean keyPressed(KeyEvent event) {
            if(event.key()!=GLFW.GLFW_KEY_LEFT&&event.key()!=GLFW.GLFW_KEY_RIGHT&&event.key()!=GLFW.GLFW_KEY_UP&&event.key()!=GLFW.GLFW_KEY_DOWN) return super.keyPressed(event);
            double step=(event.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0?.05:.01;
            double sign=event.key()==GLFW.GLFW_KEY_LEFT||event.key()==GLFW.GLFW_KEY_DOWN?-1:1;
            if(kind==0) model.hsv(model.hue(),model.saturation()+((event.key()==GLFW.GLFW_KEY_LEFT||event.key()==GLFW.GLFW_KEY_RIGHT)?sign*step:0),
                    model.value()+((event.key()==GLFW.GLFW_KEY_UP||event.key()==GLFW.GLFW_KEY_DOWN)?sign*step:0));
            else if(kind==1) model.hsv(model.hue()+sign*step,model.saturation(),model.value());
            else model.alpha(model.alpha()+(int)Math.round(sign*step*255));
            return true;
        }
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta) {
            if(kind==2) checker(g,getX(),getY(),width,height);
            if(cachedHue!=model.hue()||cachedRgb!=(model.argb()&0xFFFFFF)) {
                for(int i=0;i<colors.length;i++) colors[i]=kind==0?HsvColor.argb(model.hue(),i/(double)(colors.length-1),1,255)
                        :kind==1?HsvColor.argb(i/(double)colors.length,1,1,255):(model.argb()&0xFFFFFF)|(int)Math.round(i*255.0/(colors.length-1))<<24;
                cachedHue=model.hue();cachedRgb=model.argb()&0xFFFFFF;
            }
            for(int i=0;i<colors.length;i++) {
                int x1=getX()+i*width/colors.length,x2=getX()+(i+1)*width/colors.length;
                if(kind==0) g.fillGradient(x1,getY(),x2,getY()+height,colors[i],0xFF000000);
                else g.fill(x1,getY(),x2,getY()+height,colors[i]);
            }
            int x=getX()+(int)Math.round((width-1)*(kind==0?model.saturation():kind==1?model.hue():model.alpha()/255.0));
            int y=kind==0?getY()+(int)Math.round((height-1)*(1-model.value())):getY()+height/2;
            Draw.border(g,x-2,y-2,5,5,1,0xFFFFFFFF);
            if(isFocused()) Draw.border(g,getX()-1,getY()-1,width+2,height+2,1,globals.accentColor.get());
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE,getMessage());
            output.add(NarratedElementType.USAGE,Component.literal("Drag to change color. Arrow keys adjust; Shift makes larger steps."));
        }
    }
    private final class ChannelSlider extends AbstractSliderButton {
        private final String name;
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        private final int units;
        private double observed=Double.NaN;
        ChannelSlider(int x,int y,int width,String name,DoubleSupplier getter,DoubleConsumer setter,int units) {
            super(x,y,width,controlHeight,Component.empty(),getter.getAsDouble());this.name=name;this.getter=getter;this.setter=setter;this.units=units;updateMessage();
        }
        @Override protected void updateMessage() {setMessage(Component.literal(String.format(Locale.ROOT,"%s: %d",name,Math.round(getter.getAsDouble()*units))));}
        @Override protected void applyValue() {setter.accept(value);}
        @Override public boolean keyPressed(KeyEvent event) {
            if(canChangeValue && (event.key()==GLFW.GLFW_KEY_LEFT||event.key()==GLFW.GLFW_KEY_RIGHT)) {
                double step=((event.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0?10.0:1.0)/units;
                setValue(getter.getAsDouble()+(event.key()==GLFW.GLFW_KEY_LEFT?-step:step));return true;
            }
            return super.keyPressed(event);
        }
        @Override public void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta) {
            double current=getter.getAsDouble();
            if(current!=observed) {value=current;observed=current;updateMessage();}
            Draw.roundedRect(g,getX(),getY(),width,height,3,0xFF222E43);
            Draw.rect(g,getX()+3,getY()+height-4,(int)Math.round((width-6)*value),2,globals.accentColor.get());
            if(isFocused()) Draw.border(g,getX(),getY(),width,height,1,globals.accentColor.get());
            Draw.text(g,font,getMessage().getString(),getX()+5,getY()+2,0xFFE9EDF7,false);
        }
    }
}
