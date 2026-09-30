package dev.nexvisuals.client.hud;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.HudFeedback;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Local equipment icons and durability share the existing HUD placement/editor/scale contract. */
public final class EquipmentHudModule extends TextHudModule {
    public enum Layout { HORIZONTAL, VERTICAL }
    private final EnumSetting<Layout> layout=add(new EnumSetting<>("layout","Layout","Horizontal equipment strip or vertical cards.",Layout.HORIZONTAL,Layout.class));
    private final BooleanSetting hands=add(new BooleanSetting("hands","Include hands","Include main/off-hand items alongside four armor pieces.",true));
    private final BooleanSetting hideEmpty=add(new BooleanSetting("hide_empty","Hide empty slots","Keep only equipped items; the editor still has a selectable preview.",false));
    private final BooleanSetting bars=add(new BooleanSetting("durability_bars","Durability bars","Show remaining durability as a short bar.",true));
    private final BooleanSetting numbers=add(new BooleanSetting("durability_numbers","Durability numbers","Remaining durability percentage below each icon.",true));
    private final ColorSetting warning=add(new ColorSetting("warning_color","Low durability","ARGB color for equipment below the warning threshold.",0xFFFFAF75));
    private final DoubleSetting threshold=add(new DoubleSetting("warning_threshold","Warning threshold","Fraction of maximum durability.",.2,.05,.6));
    private final ItemStack[] items=new ItemStack[6];
    private final String[] labels=new String[6];
    private final double[] durability=new double[6];
    private int count;
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND};
    public EquipmentHudModule() {
        super("hud_equipment","Equipment HUD","Your equipped armor and held items with durability. Reads only your own inventory.",170,"Equipment");
        java.util.Arrays.fill(labels,"");
        preset("Equipment Strip","Armor plus both hands with durability bars and numbers.");
        preset("Armor Cards","Four vertical armor cards.","layout","VERTICAL","hands",false);
        preset("Minimal Gear","Compact icons and bars without empty slots or numbers.","hide_empty",true,"durability_numbers",false,"scale",.85);
    }
    @Override public void tick(Minecraft client) {
        count=0;
        if(!canDisplay(client)) {text="";return;}
        for(int i=0;i<(hands.get()?6:4);i++) {
            ItemStack stack=client.player.getItemBySlot(SLOTS[i]);
            if(hideEmpty.get()&&stack.isEmpty()) continue;
            items[count]=stack;durability[count]=HudFeedback.durability(stack.getDamageValue(),stack.getMaxDamage());
            labels[count]=stack.isDamageableItem()?Math.round(durability[count]*100)+"%":"";count++;
        }
        text="Equipment";
    }
    private int cellHeight() { return numbers.get()?35:25; }
    @Override protected int contentWidth(Minecraft client) { return count==0?client.font.width("No gear"):layout.get()==Layout.HORIZONTAL?count*29:50; }
    @Override protected int contentHeight(Minecraft client) { return layout.get()==Layout.HORIZONTAL?cellHeight():Math.max(1,count)*cellHeight(); }
    @Override protected void drawContent(Minecraft client,GuiGraphics g,int color,boolean shadow) {
        if(count==0) {Draw.text(g,client.font,"No gear",5,4,color,shadow);return;}
        for(int i=0;i<count;i++) {
            int x=5+(layout.get()==Layout.HORIZONTAL?i*29:0),y=4+(layout.get()==Layout.VERTICAL?i*cellHeight():0);
            Draw.roundedRect(g,x,y,25,20,3,0x443E4D66);
            if(!items[i].isEmpty()) g.renderItem(items[i],x+4,y+1);
            else Draw.text(g,client.font,"-",x+10,y+6,0xFF778399,false);
            int tint=durability[i]<=threshold.get()?warning.get():color;
            if(bars.get()&&items[i].isDamageableItem()) {
                Draw.rect(g,x+3,y+19,19,2,0x88303B4D);
                Draw.rect(g,x+3,y+19,(int)Math.round(19*durability[i]),2,tint);
            }
            if(numbers.get()) Draw.text(g,client.font,labels[i],x+(25-client.font.width(labels[i]))/2,y+23,tint,shadow);
        }
    }
}
