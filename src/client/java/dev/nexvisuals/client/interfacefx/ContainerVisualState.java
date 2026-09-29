package dev.nexvisuals.client.interfacefx;

import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.core.animation.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;
import java.util.List;

/** Owned by one screen. Snapshots exist only around a click, never in the render loop. */
public final class ContainerVisualState {
    private record Ghost(ItemStack item, int x, int y, int endX, int endY, boolean transfer, long start) { }
    private final ArrayDeque<Ghost> ghosts = new ArrayDeque<>(32);
    private final ArrayDeque<Hover> hovers = new ArrayDeque<>(6);
    private record Hover(int x, int y, long start) { }
    private long opened;
    private Slot lastHovered;
    private ItemStack sourceItem = ItemStack.EMPTY;
    private int[] before;
    private boolean[] eligibleDestination;
    private int sourceIndex = -1, sourceX, sourceY;

    public void reset() { opened = System.nanoTime(); ghosts.clear(); hovers.clear(); lastHovered = null; before = null; eligibleDestination = null; sourceItem = ItemStack.EMPTY; }
    public void panel(GuiGraphics g, int x, int y, int width, int height, ContainerVisualsModule module) {
        if (!module.enabled()) return;
        if (opened == 0) reset();
        float p = (float) module.easing.get().apply(progress(opened, module.duration.get()));
        int tint = Draw.withAlpha(module.panel.get(), p);
        Draw.roundedRect(g, x, y, width, height, module.rounding.get(), tint);
        if (module.outline.get()) {
            int grow = module.opening.get() == ContainerVisualsModule.Opening.SCALE ? (int) ((1-p) * 10) : 0;
            int slide = module.opening.get() == ContainerVisualsModule.Opening.SLIDE ? (int) ((1-p) * 12) : 0;
            float alpha = module.opening.get() == ContainerVisualsModule.Opening.NONE ? 1 : p;
            Draw.border(g, x - 2 - grow, y - 2 + slide - grow, width + 4 + grow * 2, height + 4 + grow * 2, 1, Draw.withAlpha(module.accent.get(), alpha));
        }
    }
    public void beforeClick(AbstractContainerMenu menu, Slot source, ClickType type, ContainerVisualsModule module) {
        before = null; eligibleDestination = null; sourceItem = ItemStack.EMPTY; sourceIndex = -1;
        if (!module.enabled() || source == null || !source.isActive()) return;
        if (module.clicks.get()) add(new Ghost(ItemStack.EMPTY, source.x, source.y, source.x, source.y, false, System.nanoTime()));
        if (!module.transfers.get() || type != ClickType.QUICK_MOVE || source.getItem().isEmpty() || menu.slots.size() > 256) return;
        sourceItem = source.getItem().copy(); sourceIndex = menu.slots.indexOf(source); sourceX = source.x; sourceY = source.y;
        before = counts(menu, sourceItem);
        eligibleDestination = new boolean[menu.slots.size()];
        for (int i = 0; i < eligibleDestination.length; i++) {
            ItemStack item = menu.slots.get(i).getItem();
            eligibleDestination[i] = item.isEmpty() || ItemStack.isSameItemSameComponents(item, sourceItem);
        }
    }
    public void afterClick(AbstractContainerMenu menu) {
        if (before == null) return;
        int[] after = counts(menu, sourceItem);
        List<Integer> destinations = TransferMatcher.destinations(sourceIndex, before, after);
        if (destinations.stream().anyMatch(index -> !eligibleDestination[index])) destinations = List.of();
        if (sourceIndex >= 0 && sourceIndex < menu.slots.size()) {
            ItemStack remaining = menu.slots.get(sourceIndex).getItem();
            if (!remaining.isEmpty() && !ItemStack.isSameItemSameComponents(remaining, sourceItem)) destinations = List.of();
        }
        for (int index : destinations) {
            Slot target = menu.slots.get(index);
            if (target.isActive()) add(new Ghost(sourceItem.copyWithCount(1), sourceX, sourceY, target.x, target.y, true, System.nanoTime()));
        }
        before = null; eligibleDestination = null; sourceItem = ItemStack.EMPTY;
    }
    private static int[] counts(AbstractContainerMenu menu, ItemStack template) {
        int[] counts = new int[menu.slots.size()];
        for (int i = 0; i < counts.length; i++) {
            ItemStack item = menu.slots.get(i).getItem();
            if (ItemStack.isSameItemSameComponents(item, template)) counts[i] = item.getCount();
        }
        return counts;
    }
    private void add(Ghost ghost) { while (ghosts.size() >= 32) ghosts.removeFirst(); ghosts.addLast(ghost); }
    private double progress(long start, int duration) { return EffectMath.unit((System.nanoTime() - start) / (duration * 1_000_000.0)); }

    public void render(GuiGraphics g, int left, int top, Slot hovered, ContainerVisualsModule module) {
        if (!module.enabled()) { ghosts.clear(); hovers.clear(); return; }
        if (hovered != lastHovered) {
            if (lastHovered != null) { if (hovers.size() >= 6) hovers.removeFirst(); hovers.addLast(new Hover(lastHovered.x, lastHovered.y, System.nanoTime())); }
            lastHovered = hovered;
        }
        g.pose().pushMatrix(); g.pose().translate(left, top);
        if (module.hover.get()) {
            if (hovered != null) Draw.border(g, hovered.x - 1, hovered.y - 1, 18, 18, 1, module.accent.get());
            hovers.removeIf(h -> progress(h.start, module.duration.get()) >= 1);
            for (Hover h : hovers) Draw.border(g, h.x - 1, h.y - 1, 18, 18, 1,
                    Draw.withAlpha(module.accent.get(), (float) (1 - module.easing.get().apply(progress(h.start, module.duration.get())))));
        }
        ghosts.removeIf(ghost -> progress(ghost.start, module.duration.get()) >= 1);
        for (Ghost ghost : ghosts) {
            double p = progress(ghost.start, module.duration.get()), t = module.easing.get().apply(p);
            float alpha = (float) (1 - p);
            if (!ghost.transfer) {
                int grow = (int) (p * 6 * module.intensity.get());
                Draw.border(g, ghost.x - grow, ghost.y - grow, 16 + grow*2, 16 + grow*2, 1, Draw.withAlpha(module.accent.get(), alpha));
                continue;
            }
            var style = module.motion.get();
            if (style == ContainerVisualsModule.Motion.SLIDE) t = p;
            if (style == ContainerVisualsModule.Motion.FADE) {
                Draw.rect(g, ghost.endX, ghost.endY, 16, 16, Draw.withAlpha(module.accent.get(), alpha * .5f));
                continue;
            }
            float x = (float) (ghost.x + (ghost.endX - ghost.x) * t), y = (float) (ghost.y + (ghost.endY - ghost.y) * t);
            if (style == ContainerVisualsModule.Motion.ARC) y -= (float) (EffectMath.arc(t) * 24 * module.intensity.get());
            float scale = module.scale.get().floatValue() * (float) Math.min(1, (1-p) * 5);
            if (style == ContainerVisualsModule.Motion.POP) scale *= (float) (1 + .35 * EffectMath.arc(t) * module.intensity.get());
            g.pose().pushMatrix(); g.pose().translate(x+8, y+8); g.pose().scale(scale, scale);
            // GuiGraphics has no safe general item-opacity multiplier in 1.21.11. Shrink out instead.
            g.renderItem(ghost.item, -8, -8); g.pose().popMatrix();
            if (module.afterimage.get()) Draw.border(g, (int) x - 2, (int) y - 2, 20, 20, 1, Draw.withAlpha(module.accent.get(), alpha * .5f));
        }
        g.pose().popMatrix();
    }
}
