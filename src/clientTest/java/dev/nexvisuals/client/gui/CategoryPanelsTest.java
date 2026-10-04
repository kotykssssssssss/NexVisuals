package dev.nexvisuals.client.gui;

import dev.nexvisuals.core.config.GlobalSettings;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.ui.PanelPosition;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.input.*;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CategoryPanelsTest {
    @Test void everyCategoryRemainsReachableWithoutOverlappingGridWindowsAtDifferentGuiScales() {
        for (int[] size:new int[][]{{320,180},{480,270},{640,360},{960,540},{1920,1080}}) {
            int visited=0;
            var first=CategoryBoardLayout.of(size[0],size[1],Category.values().length,0);
            for(int page=0; page<first.pages(); page++) {
                var layout=CategoryBoardLayout.of(size[0],size[1],Category.values().length,page);
                visited+=layout.slots().size();
                for(var slot:layout.slots()) {
                    assertTrue(slot.width()>=120); assertTrue(slot.height()>=72);
                    assertTrue(slot.x()>=layout.workspace().x() && slot.right()<=layout.workspace().right());
                    assertTrue(slot.y()>=layout.workspace().y() && slot.bottom()<=layout.workspace().bottom());
                }
                for(int i=0;i<layout.slots().size();i++) for(int j=i+1;j<layout.slots().size();j++) {
                    var a=layout.slots().get(i); var b=layout.slots().get(j);
                    assertTrue(a.right()<=b.x() || b.right()<=a.x() || a.bottom()<=b.y() || b.bottom()<=a.y());
                }
                assertTrue(layout.inspector().x()>=0 && layout.inspector().right()<=size[0]);
                assertTrue(layout.inspector().y()>=0 && layout.inspector().bottom()<=size[1]);
            }
            assertEquals(Category.values().length,visited);
            assertEquals(0,CategoryBoardLayout.of(size[0],size[1],9,-9).page());
            assertEquals(first.pages()-1,CategoryBoardLayout.of(size[0],size[1],9,999).page());
        }
        assertTrue(CategoryBoardLayout.of(320,180,0,0).slots().isEmpty());
    }
    @Test void moduleRowsSeparateTogglingFromSettingsAndObeyClippingWithoutTooltipRecreation() throws Exception {
        var globals=new GlobalSettings(); var module=module("a"); var opens=new AtomicInteger();
        var row=new ModuleRow(14,140,module,globals,opens::incrementAndGet);
        var pane=new ScrollPane(new GuiLayout.Rect(10,20,160,70),0); pane.add(row,4);
        var tooltip=tooltip(row);
        // Right click and the explicit settings affordance never enable the module.
        assertTrue(row.mouseClicked(mouse(40,30,1),false)); assertFalse(module.enabled());
        assertTrue(row.mouseClicked(mouse(145,30,0),false)); assertFalse(module.enabled()); assertEquals(2,opens.get());
        row.setFocused(true); assertTrue(row.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT,0,0))); assertEquals(3,opens.get());
        assertFalse(row.mouseClicked(mouse(40,95,1),false));
        pane.moveTo(new GuiLayout.Rect(70,40,160,70));
        assertEquals(74,row.getX()); assertEquals(44,row.getY());
        assertFalse(row.isMouseOver(20,30)); assertTrue(row.isMouseOver(90,50));
        assertSame(tooltip,tooltip(row));
        row.active=false; assertFalse(row.mouseClicked(mouse(90,50,1),false));
        row.onPress(null); assertTrue(module.enabled()); row.onPress(null); assertFalse(module.enabled());
    }
    @Test void panelDraggingClampsToWorkspaceAndPersistedPoseRestoresAfterResize() {
        var globals=new GlobalSettings(); var widgets=new ArrayList<AbstractWidget>(); var positions=new ArrayList<PanelPosition>();
        var rebuilds=new AtomicInteger();
        var area=new GuiLayout.Rect(10,65,400,200);
        var panel=new CategoryPanel(Category.WORLD,new GuiLayout.Rect(10,65,180,160),area,null,0,
                List.of(module("a")),globals,true,widgets::add,id->{},positions::add,rebuilds::incrementAndGet);
        panel.move(999,-999);
        assertEquals(new GuiLayout.Rect(230,65,180,160),panel.bounds());
        assertEquals(new PanelPosition(1,0,false),positions.getLast());
        panel.toggle(); assertEquals(24,panel.bounds().height()); assertTrue(positions.getLast().collapsed()); assertEquals(1,rebuilds.get());
        var resized=new CategoryPanel(Category.WORLD,new GuiLayout.Rect(6,65,140,100),new GuiLayout.Rect(6,65,308,100),positions.getLast(),0,
                List.of(module("a")),globals,true,widgets::add,id->{},positions::add,rebuilds::incrementAndGet);
        assertEquals(new GuiLayout.Rect(174,65,140,24),resized.bounds());
    }
    @Test void scrolledNativeTextInputsCannotCaptureClicksAboveTheSettingsViewport() {
        var pane=new ScrollPane(new GuiLayout.Rect(10,40,160,80),0);
        var field=new net.minecraft.client.gui.components.EditBox(null,14,0,140,20,net.minecraft.network.chat.Component.literal("Text"));
        pane.add(field,-22);
        assertEquals(18,field.getY());
        assertNull(pane.click(mouse(20,25,0),false));
        assertFalse(field.isFocused());
    }
    @Test void queryCursorNotificationsKeepIndependentPanelScrollAndSavedPositions() {
        var globals=new GlobalSettings(); globals.setPanelPosition(Category.WORLD,new PanelPosition(.6,.2,true));
        var state=new GuiState(globals); state.updateQuery("trail"); state.panelScroll.put(Category.WORLD,96); state.boardPage=1;
        assertFalse(state.updateQuery("trail")); assertEquals(96,state.panelScroll.get(Category.WORLD)); assertEquals(1,state.boardPage);
        assertTrue(state.updateQuery("trails")); assertTrue(state.panelScroll.isEmpty()); assertEquals(0,state.boardPage);
        assertEquals(new PanelPosition(.6,.2,true),state.panelPositions.get(Category.WORLD));
        state.remember(globals); assertEquals(state.panelPositions,globals.panelPositions());
        var registry=new ModuleRegistry(); registry.register(module("a"));
        registry.register(new VisualModule("hud_trail","HUD Trail","Cosmetic",Category.HUD){});
        state.query="trail"; state.category=Category.COMBAT;
        assertEquals(1,state.matching(registry,Category.HUD).size()); assertTrue(state.matching(registry).isEmpty());
    }
    private VisualModule module(String id) { return new VisualModule(id,"Module "+id,"Cosmetic",Category.WORLD){}; }
    private MouseButtonEvent mouse(double x,double y,int button) { return new MouseButtonEvent(x,y,new MouseButtonInfo(button,0)); }
    private Object tooltip(AbstractWidget widget) throws Exception {
        var field=AbstractWidget.class.getDeclaredField("tooltip"); field.setAccessible(true);
        return ((WidgetTooltipHolder)field.get(widget)).get();
    }
}
