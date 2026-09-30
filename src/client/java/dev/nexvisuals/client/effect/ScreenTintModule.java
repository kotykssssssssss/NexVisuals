package dev.nexvisuals.client.effect;

import dev.nexvisuals.client.module.HudModule;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.ColorSetting;
import dev.nexvisuals.core.setting.IntSetting;
import dev.nexvisuals.core.setting.DoubleSetting;
import dev.nexvisuals.core.setting.EnumSetting;
import dev.nexvisuals.core.animation.HudFeedback;
import dev.nexvisuals.core.hud.ReticleMotion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Adds a cosmetic edge tint; does not remove vanilla hurt, fire, fog or other gameplay cues. */
public final class ScreenTintModule extends VisualModule implements HudModule {
    public enum Mode { STATIC, LOW_HEALTH, DAMAGE, BREATHE }
    private final EnumSetting<Mode> mode = add(new EnumSetting<>("mode", "Behavior", "Static edges, local health heartbeat, actual local hurt state, or slow breathing.", Mode.STATIC, Mode.class));
    private final DoubleSetting intensity = add(new DoubleSetting("intensity", "Intensity", "Multiplies the edge alpha.", 1, 0, 1));
    private final DoubleSetting threshold = add(new DoubleSetting("health_threshold", "Low health threshold", "Fraction of your own maximum health below which the heartbeat appears.", .35, .1, .8));
    private final DoubleSetting speed = add(new DoubleSetting("pulse_speed", "Pulse speed", "Heartbeat/breathing cycles per second.", 1.2, .3, 3));
    private final ColorSetting color = add(new ColorSetting("color", "Edge color", "ARGB edge tint. Keep alpha low for a subtle result.", 0x40395A88));
    private final IntSetting size = add(new IntSetting("size", "Edge width", "Width of the fading border in GUI pixels.", 36, 4, 100));
    public ScreenTintModule() {
        super("screen_tint", "Screen Edge Tint", "Cosmetic edges with optional local health/damage pulse; preserves vanilla cues.", Category.WORLD);
        preset("Soft Edges", "The original static edge tint.");
        preset("Low HP Pulse", "A double heartbeat that grows as your own health drops.", "mode", "LOW_HEALTH", "color", "#BCFF526F", "size", 65, "intensity", .8);
        preset("Damage Pulse", "Brief warm edges while your own vanilla hurt timer is active.", "mode", "DAMAGE", "color", "#99FF917A", "size", 55);
        preset("Calm Breath", "Slow cool atmospheric edges.", "mode", "BREATHE", "color", "#706399D7", "pulse_speed", .4);
    }
    @Override public boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (client.player == null || client.level == null) return false;
        double seconds=(client.level.getGameTime()+deltaTracker.getGameTimeDeltaPartialTick(false))/20.0;
        double amount=intensity.get()*switch(mode.get()) {
            case STATIC -> 1;
            case LOW_HEALTH -> HudFeedback.lowHealth(client.player.getHealth(),client.player.getMaxHealth(),threshold.get())*HudFeedback.heartbeat(seconds,speed.get());
            case DAMAGE -> Math.clamp(client.player.hurtTime/10.0,0,1);
            case BREATHE -> .2+.8*ReticleMotion.breathe(seconds,speed.get());
        };
        if(amount<=0) return false;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        int edge = Math.min(size.get(), Math.min(w, h) / 2);
        // Nested disjoint outlines avoid multiplying alpha at corners.
        for (int i = 0; i < edge; i++) {
            float opacity = 1 - (float) i / edge;
            Draw.border(graphics, i, i, w - i * 2, h - i * 2, 1, Draw.withAlpha(color.get(), opacity * opacity * (float)amount));
        }
        return true;
    }
}
