package dev.nexvisuals.client.hud;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.Easing;
import dev.nexvisuals.core.hud.Anchor;
import dev.nexvisuals.core.hud.PickupFeed;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** A transient feed of confirmed local pickup events, using the existing draggable HUD contract. */
public final class PickupHudModule extends TextHudModule {
    public enum Animation { SLIDE, POP, FADE }
    private final EnumSetting<Animation> animation = add(new EnumSetting<>("animation", "Animation", "Slide, gentle pop or a stationary text fade.", Animation.SLIDE, Animation.class));
    private final DoubleSetting duration = add(new DoubleSetting("duration", "Duration", "Seconds each notice stays visible; collecting the same stack refreshes it.", 3, .75, 8));
    private final IntSetting rows = add(new IntSetting("rows", "Visible rows", "At most six transient notifications. Older rows are discarded.", 4, 1, PickupFeed.MAX_ROWS));
    private final BooleanSetting icons = add(new BooleanSetting("icons", "Item icons", "Use the installed resource pack's real item icons.", true));
    private final BooleanSetting names = add(new BooleanSetting("names", "Item names", "Include item names beside the collected amount.", true));
    private final IntSetting labelWidth = add(new IntSetting("label_width", "Text width", "Maximum GUI width of each label before clipping.", 150, 48, 240));
    private final ColorSetting accent = add(new ColorSetting("accent", "Accent", "ARGB accent beside each row.", 0xFF8B9DFF));
    private record PickupItem(ItemStack icon, String name) { }
    private final PickupFeed<PickupItem> feed = new PickupFeed<>();
    private final String[] labels = new String[PickupFeed.MAX_ROWS];
    private Object level;
    private long lastTick;
    private double renderTick;
    private long labelRevision = -1;
    private boolean labelNames;
    private int cachedLabelWidth;

    public PickupHudModule() {
        super("hud_pickups", "Pickup HUD", "Animated notifications for your own server-confirmed item pickups. No inventory automation or remote tracking.", 52, "Pickups appear here", Anchor.TOP_RIGHT);
        preset("Stacked", "Item icons, names and a smooth slide-in.");
        preset("Compact", "Small count-only item cards.", "names", false, "rows", 3, "scale", .85, "animation", "POP");
        preset("Minimal", "Quiet text notices without icons or a background.", "icons", false, "background", false, "animation", "FADE", "rows", 3, "duration", 2.5);
        group("Notifications", animation, duration, rows, icons, names, labelWidth, accent);
    }

    public void pickedUp(Minecraft client, ItemStack stack, int amount) {
        if (!enabled() || !canDisplay(client) || stack.isEmpty() || amount <= 0) return;
        if (level != client.level) { clearFeed(); level = client.level; }
        lastTick = client.level.getGameTime();
        feed.offer(new PickupItem(stack.copyWithCount(1), stack.getHoverName().getString()), amount, lastTick,
                lifetimeTicks(), rows.get(), (a, b) -> ItemStack.isSameItemSameComponents(a.icon(), b.icon()));
        refreshLabels(client);
    }

    private int lifetimeTicks() { return (int) Math.round(duration.get() * 20); }
    @Override public void tick(Minecraft client) {
        if (!canDisplay(client) || level != client.level) { clearFeed(); level = client.level; }
        if (!canDisplay(client) || client.isPaused()) return;
        lastTick = client.level.getGameTime();
        feed.prune(lastTick, lifetimeTicks(), rows.get());
        refreshLabels(client);
    }
    private void refreshLabels(Minecraft client) {
        if (labelRevision == feed.revision() && labelNames == names.get() && cachedLabelWidth == labelWidth.get()) return;
        labelRevision = feed.revision(); labelNames = names.get(); cachedLabelWidth = labelWidth.get();
        var notices = feed.notices();
        for (int i = 0; i < notices.size(); i++) {
            var notice = notices.get(i);
            String amount = notice.amount() == PickupFeed.MAX_AMOUNT ? "9999+" : Integer.toString(notice.amount());
            String label = "+" + amount + (labelNames ? " " + notice.item().name() : "");
            labels[i] = client.font.plainSubstrByWidth(label, cachedLabelWidth);
        }
        text = notices.isEmpty() ? "" : "Pickups";
    }

    @Override public boolean renderHud(Minecraft client, GuiGraphics graphics, DeltaTracker delta) {
        renderTick = lastTick + (client.isPaused() ? 0 : delta.getGameTimeDeltaPartialTick(false));
        return super.renderHud(client, graphics, delta);
    }
    @Override protected int contentWidth(Minecraft client) {
        int width = client.font.width("Pickups appear here");
        if (!feed.notices().isEmpty()) {
            width = 0;
            for (int i = 0; i < feed.notices().size(); i++) width = Math.max(width, Math.min(labelWidth.get(), client.font.width(labels[i])));
            if (icons.get()) width += 24;
        }
        return width + 10;
    }
    @Override protected int contentHeight(Minecraft client) { return Math.max(1, feed.notices().size()) * 23; }

    @Override protected void drawContent(Minecraft client, GuiGraphics graphics, int color, boolean shadow) {
        var notices = feed.notices();
        if (notices.isEmpty()) { Draw.text(graphics, client.font, "Pickups appear here", 5, 9, color, shadow); return; }
        boolean preview = client.screen instanceof dev.nexvisuals.client.gui.HudEditorScreen;
        for (int i = 0; i < notices.size(); i++) {
            var notice = notices.get(i);
            double age = preview ? 8 : Math.max(0, renderTick - notice.tick());
            float alpha = (float) PickupFeed.opacity(age, lifetimeTicks());
            double enter = Easing.OUT_CUBIC.apply(Math.clamp(age / 5, 0, 1));
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate((float) (animation.get() == Animation.SLIDE ? (1 - enter) * 12 : 0), i * 23f);
                if (animation.get() == Animation.POP) {
                    float pop = (float) (.8 + .2 * enter);
                    graphics.pose().translate(5, 12);
                    graphics.pose().scale(pop, pop);
                    graphics.pose().translate(-5, -12);
                }
                Draw.roundedRect(graphics, 5, 4, 2, 18, 1, Draw.withAlpha(accent.get(), alpha));
                int x = 11;
                if (icons.get()) { graphics.renderItem(notice.item().icon(), x, 5); x += 24; }
                Draw.text(graphics, client.font, labels[i], x, 9, Draw.withAlpha(color, alpha), shadow);
            } finally { graphics.pose().popMatrix(); }
        }
    }

    private void clearFeed() { feed.clear(); java.util.Arrays.fill(labels, null); text = ""; lastTick = 0; renderTick = 0; labelRevision = -1; }
    @Override protected void onDisable() { clearFeed(); level = null; }
}
