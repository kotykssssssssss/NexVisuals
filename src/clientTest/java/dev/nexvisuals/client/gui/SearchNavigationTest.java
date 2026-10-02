package dev.nexvisuals.client.gui;

import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.module.VisualModule;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Real 1.21.11 text/cursor events and pane geometry, without Minecraft, a font renderer or a GPU. */
class SearchNavigationTest {
    private EditBox search(GuiState state, AtomicInteger rebuilds) {
        var box = new EditBox(null, 0, 0, 200, 20, Component.literal("Search"));
        box.setMaxLength(80);
        box.setValue(state.query);
        box.setResponder(value -> {
            if (state.updateQuery(value)) rebuilds.incrementAndGet();
        });
        return box;
    }

    @Test void restoringTheSearchCursorDoesNotQueueAnotherRebuildOrResetScroll() {
        var state = new GuiState(new GlobalSettings());
        var rebuilds = new AtomicInteger();
        var box = search(state, rebuilds);
        box.insertText("trail");
        assertEquals(1, rebuilds.get());
        state.moduleScroll = 112;

        // NexVisualsScreen recreates the field then restores focus/cursor after a list rebuild.
        var restored = search(state, rebuilds);
        var nativeNotifications = new AtomicInteger();
        restored.setResponder(value -> {
            nativeNotifications.incrementAndGet();
            if (state.updateQuery(value)) rebuilds.incrementAndGet();
        });
        restored.setFocused(true);
        for (int tick = 0; tick < 60; tick++) restored.moveCursorTo(box.getCursorPosition(), false);

        assertEquals(60, nativeNotifications.get(), "Minecraft notifies even when the text is unchanged");
        assertEquals(1, rebuilds.get(), "Focus restoration must not start an endless rebuild loop");
        assertEquals(112, state.moduleScroll);
        assertEquals("trail", restored.getValue());
        assertEquals(5, restored.getCursorPosition());
        assertTrue(restored.isFocused());
    }

    @Test void editingAndClearingFilterOnceButCursorAndSelectionChangesKeepNavigation() {
        var state = new GuiState(new GlobalSettings());
        state.selectedId = "player_trails";
        state.settingScroll = 45;
        var rebuilds = new AtomicInteger();
        var box = search(state, rebuilds);
        box.insertText("trails");
        state.moduleScroll = 84;
        box.moveCursorToStart(false);
        box.moveCursorTo(3, true);
        assertEquals("tra", box.getHighlighted());
        box.moveCursorToEnd(false);
        box.setValue("trails");
        assertEquals(1, rebuilds.get());
        assertEquals(84, state.moduleScroll);

        box.moveCursorToEnd(false);
        box.deleteChars(-1);
        assertEquals("trail", state.query);
        assertEquals(2, rebuilds.get());
        assertEquals(0, state.moduleScroll);
        state.moduleScroll = 28;
        box.setValue("");
        assertEquals("", state.query);
        assertEquals(3, rebuilds.get());
        assertEquals(0, state.moduleScroll);
        assertEquals("player_trails", state.selectedId);
        assertEquals(45, state.settingScroll);
    }

    @Test void filteredRowsRemainScrollableWithStableTooltipsAndCorrectHitAreas() throws ReflectiveOperationException {
        var globals = new GlobalSettings();
        var state = new GuiState(globals);
        state.category = null;
        var registry = new ModuleRegistry();
        for (int i = 0; i < 12; i++) registry.register(module("trail_" + i, "Trail " + i, "Cosmetic trail", Category.WORLD));
        registry.register(module("other", "Other", "Unrelated", Category.HUD));
        var rebuilds = new AtomicInteger();
        var box = search(state, rebuilds);
        box.insertText("trail");
        var matches = state.matching(registry);
        assertEquals(12, matches.size());
        var area = new GuiLayout.Rect(10, 20, 190, 100);
        var pane = new ScrollPane(area, state.moduleScroll);
        NexButton last = null;
        for (int i = 0; i < matches.size(); i++) {
            var module = matches.get(i);
            var button = new NexButton(15, 0, 170, 23, module::name, () -> false, () -> {}, globals);
            button.setTooltip(Tooltip.create(Component.literal(module.description())));
            pane.add(button, 5 + i * 53);
            pane.decorate(5 + i * 53, 47, (graphics, y) -> {});
            last = button;
        }
        assertNotNull(last);
        var tooltip = tooltip(last);
        assertNotNull(tooltip);
        pane.scrollBy(10_000);
        assertEquals(541, pane.scroll());
        state.moduleScroll = pane.scroll();
        box.moveCursorToStart(false);
        box.moveCursorToEnd(false);
        assertEquals(541, state.moduleScroll);
        assertEquals(1, rebuilds.get());
        assertTrue(last.isMouseOver(30, last.getY() + 5));
        assertFalse(last.isMouseOver(30, area.bottom() + 5));
        assertSame(tooltip, tooltip(last), "Scrolling must keep the existing description widget");
        pane.scrollBy(-28);
        assertEquals(513, pane.scroll());
        pane.scrollBy(-10_000);
        assertEquals(0, pane.scroll());
        assertFalse(last.isMouseOver(30, last.getY() + 5), "Offscreen rows cannot receive pane input");
        assertSame(tooltip, tooltip(last));
    }

    @Test void queryChangesStillFilterNamesDescriptionsAndIdsWithinTheSelectedCategory() {
        var state = new GuiState(new GlobalSettings());
        var registry = new ModuleRegistry();
        var nameMatch = registry.register(module("player_trails", "Player Trails", "Local cosmetic", Category.WORLD));
        var descriptionMatch = registry.register(module("spark", "Spark", "Soft trail glow", Category.WORLD));
        var idMatch = registry.register(module("weapon_trail", "Weapon", "Local cosmetic", Category.VIEWMODEL));
        state.category = null;
        assertTrue(state.updateQuery("  TRAIL  "));
        assertEquals(java.util.List.of(nameMatch, descriptionMatch, idMatch), state.matching(registry));
        state.category = Category.WORLD;
        assertEquals(java.util.List.of(nameMatch, descriptionMatch), state.matching(registry));
        assertTrue(state.updateQuery("missing"));
        assertTrue(state.matching(registry).isEmpty());
        assertTrue(state.updateQuery(""));
        assertEquals(java.util.List.of(nameMatch, descriptionMatch), state.matching(registry));
    }

    private VisualModule module(String id, String name, String description, Category category) {
        return new VisualModule(id, name, description, category) {};
    }

    private Tooltip tooltip(AbstractWidget widget) throws ReflectiveOperationException {
        // 1.21.11 exposes setTooltip but no getter; introspection stays in this headless test.
        var field = AbstractWidget.class.getDeclaredField("tooltip");
        field.setAccessible(true);
        return ((WidgetTooltipHolder) field.get(widget)).get();
    }
}
