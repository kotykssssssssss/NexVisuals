package dev.nexvisuals.client.gui;

import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises transitions without a game tick; drawing itself still needs manual testing. */
class GuiLifecycleTest {
    @Test void settingsClickedBetweenTicksAreReadyForTheNextFrameForEnabledAndDisabledModules() {
        for (boolean enabled : new boolean[]{false, true}) {
            var globals = new GlobalSettings();
            var state = new GuiState(globals);
            var module = new VisualModule("test", "Test", "Cosmetic", Category.WORLD) {};
            module.setEnabled(enabled);
            var pane = new AtomicReference<ScrollPane>();
            var builds = new AtomicInteger();
            var row = row(module, globals, state);

            assertTrue(row.mouseClicked(new MouseButtonEvent(30, 8, new MouseButtonInfo(1, 0)), false));
            assertTrue(state.narrowDetails);
            assertNull(pane.get(), "the click happens after the last tick, before rendering");
            Runnable build = () -> {
                builds.incrementAndGet();
                pane.set(new ScrollPane(new GuiLayout.Rect(10, 40, 160, 80), 0));
            };
            state.rebuildIfRequested(build);
            assertNotNull(pane.get(), "the upcoming frame must have its settings pane");
            assertEquals("test", state.selectedId);
            assertEquals(enabled, module.enabled(), "opening settings must not toggle a module");
            state.rebuildIfRequested(build);
            assertEquals(1, builds.get(), "idle frames must keep the same widgets");
        }
    }

    @Test void keyboardOpenAndCloseFollowedByAnotherInputDoNotNeedATick() {
        var globals = new GlobalSettings();
        var state = new GuiState(globals);
        var module = new VisualModule("test", "Test", "Cosmetic", Category.HUD) {};
        var row = row(module, globals, state);
        var editorVisible = new AtomicReference<Boolean>(false);
        Runnable build = () -> editorVisible.set(state.narrowDetails);
        row.setFocused(true);
        assertTrue(row.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0)));
        state.rebuildIfRequested(build);
        assertTrue(editorVisible.get());
        state.narrowDetails = false;
        state.requestRebuild();
        state.rebuildIfRequested(build); // next input, still no tick
        assertFalse(editorVisible.get());
        assertFalse(module.enabled());
    }

    @Test void rapidSearchChangesCoalesceButARequestDuringRebuildingIsKept() {
        var state = new GuiState(new GlobalSettings());
        var renderedQuery = new AtomicReference<String>();
        var builds = new AtomicInteger();
        for (String query : new String[]{"t", "tr", "trail"}) {
            assertTrue(state.updateQuery(query));
            state.requestRebuild();
        }
        state.rebuildIfRequested(() -> {
            renderedQuery.set(state.query);
            builds.incrementAndGet();
            assertFalse(state.updateQuery("trail"));
            state.requestRebuild(); // a newly requested update survives the current one
        });
        assertEquals("trail", renderedQuery.get());
        assertEquals(1, builds.get());
        state.rebuildIfRequested(builds::incrementAndGet);
        state.rebuildIfRequested(builds::incrementAndGet);
        assertEquals(2, builds.get());
    }

    @Test void actualScreenPreparesWidgetsBeforeRenderingOrRoutingAnyNextInput() throws Exception {
        var entryPoints = Set.of("render", "tick", "mouseClicked", "mouseScrolled", "mouseDragged",
                "mouseReleased", "charTyped", "keyPressed", "keyReleased");
        try (var stream = getClass().getClassLoader().getResourceAsStream(
                "dev/nexvisuals/client/gui/NexVisualsScreen.class")) {
            assertNotNull(stream);
            var screen = new ClassNode();
            new ClassReader(stream).accept(screen, 0);
            int checked = 0;
            for (var method : screen.methods) {
                if (!entryPoints.contains(method.name)) continue;
                var first = method.instructions.getFirst();
                while (first.getOpcode() < 0) first = first.getNext();
                assertEquals(Opcodes.ALOAD, first.getOpcode(), method.name);
                var call = first.getNext();
                while (call.getOpcode() < 0) call = call.getNext();
                assertInstanceOf(MethodInsnNode.class, call, method.name);
                assertEquals(screen.name, ((MethodInsnNode) call).owner, method.name);
                assertEquals("prepareWidgets", ((MethodInsnNode) call).name,
                        method.name + " must flush an opening/closing request before touching old widgets");
                checked++;
            }
            assertEquals(entryPoints.size(), checked);
        }
    }

    private ModuleRow row(VisualModule module, GlobalSettings globals, GuiState state) {
        return new ModuleRow(0, 140, module, globals, () -> {
            state.selectedId = module.id();
            state.narrowDetails = true;
            state.requestRebuild();
        });
    }
}
