package dev.nexvisuals.client.hud;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.*;

public final class ItemCounterModule extends TextHudModule {
    public enum ItemChoice { HELD, TOTEMS, PEARLS, ARROWS, ROCKETS }
    private final EnumSetting<ItemChoice> choice=add(new EnumSetting<>("item","Count item","Match item type in your 36 inventory slots and offhand; no container scanning or automation.",ItemChoice.TOTEMS,ItemChoice.class));
    private final BooleanSetting icon=add(new BooleanSetting("icon","Item icon","Draw the installed resource-pack item model.",true));
    private final BooleanSetting label=add(new BooleanSetting("label","Item name","Include the localized item name.",true));
    // Item registries are accessed on the first in-world tick, never while constructing metadata.
    private ItemStack display;
    private int ticks;
    public ItemCounterModule() {
        super("hud_item_counter","Item Counter","Your own carried items, with an icon and total count. Never reads another player's inventory.",212,"Totems: 3");
        preset("Totems","Count carried totems.");
        preset("Pearls","Compact pearl counter.","item","PEARLS","label",false);
        preset("Held Item","Count stacks matching the item in your main hand.","item","HELD");
    }
    @Override public void tick(Minecraft client) {
        if(!canDisplay(client)) {text="";display=ItemStack.EMPTY;return;}
        if(!text.isEmpty()&&++ticks%4!=0) return;
        Item selected=switch(choice.get()) {
            case HELD -> client.player.getMainHandItem().getItem();
            case TOTEMS -> Items.TOTEM_OF_UNDYING;case PEARLS -> Items.ENDER_PEARL;
            case ARROWS -> Items.ARROW;case ROCKETS -> Items.FIREWORK_ROCKET;
        };
        int amount=0;
        for(int i=0;i<36;i++) {var stack=client.player.getInventory().getItem(i);if(stack.is(selected)) amount+=stack.getCount();}
        var off=client.player.getOffhandItem();if(off.is(selected)) amount+=off.getCount();
        display=selected==Items.AIR?ItemStack.EMPTY:new ItemStack(selected);
        text=(label.get()&&!display.isEmpty()?display.getHoverName().getString()+": ":"")+amount;
    }
    @Override protected int contentWidth(Minecraft client) { return super.contentWidth(client)+(icon.get()?20:0); }
    @Override protected int contentHeight(Minecraft client) { return icon.get()?18:super.contentHeight(client); }
    @Override protected void drawContent(Minecraft client,GuiGraphics g,int color,boolean shadow) {
        if(icon.get()&&display!=null&&!display.isEmpty()) g.renderItem(display,5,4);
        Draw.text(g,client.font,displayedText(),icon.get()?25:5,icon.get()?8:4,color,shadow);
    }
}
