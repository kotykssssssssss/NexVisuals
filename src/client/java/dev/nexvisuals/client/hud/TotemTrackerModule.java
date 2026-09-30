package dev.nexvisuals.client.hud;

import dev.nexvisuals.core.hud.ActivationCounter;
import dev.nexvisuals.core.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

/** Real local activations, distinct from Item Counter's carried totem quantity. */
public final class TotemTrackerModule extends TextHudModule {
    private final BooleanSetting timer=add(new BooleanSetting("last_pop_time","Time since pop","Show elapsed real seconds since your last activation.",true));
    private final BooleanSetting resetWorld=add(new BooleanSetting("reset_on_world","Reset on world change","Reset session counts when changing world/dimension. Counts are not stored in profiles.",true));
    private final BooleanSetting compact=add(new BooleanSetting("compact","Compact label","Use a short counter label.",false));
    private final ActivationCounter counter=new ActivationCounter();
    private int ticks;
    public TotemTrackerModule() {
        super("hud_totem_tracker","Totem Tracker","Counts actual activations of your own totem while enabled; no remote tracking or guessed deaths.",286,"Totem pops: 0 | Last: -");
        preset("Timeline","Activation count and elapsed time.");
        preset("Counter","Just the activation count.","last_pop_time",false);
        preset("Minimal","Compact text without a panel.","compact",true,"last_pop_time",false,"background",false);
        action("Reset counter","Reset this session's activation count, keeping HUD placement/settings.",()->{counter.reset();text="";});
    }
    public void popped(Minecraft client) {
        if(!enabled() || !canDisplay(client)) return;
        counter.record(client.level,System.nanoTime(),resetWorld.get());text="";
    }
    @Override public void tick(Minecraft client) {
        if(!canDisplay(client)) {text="";counter.observeLevel(null,resetWorld.get());return;}
        counter.observeLevel(client.level,resetWorld.get());
        if(!text.isEmpty() && ++ticks%20!=0) return;
        text=(compact.get()?"Pops: ":"Totem pops: ")+counter.count();
        if(timer.get()) text+=" | Last: "+(counter.count()==0?"-":counter.elapsedSeconds(System.nanoTime())+"s");
    }
}
