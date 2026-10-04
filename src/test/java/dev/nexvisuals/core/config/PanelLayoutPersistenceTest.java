package dev.nexvisuals.core.config;

import com.google.gson.JsonParser;
import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.ModuleRegistry;
import dev.nexvisuals.core.ui.PanelPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PanelLayoutPersistenceTest {
    @TempDir Path directory;
    @Test void normalizedPlacementIsClampedAndNonFiniteInputRejected() {
        assertEquals(new PanelPosition(0,1,true),new PanelPosition(-1,2,true));
        assertThrows(IllegalArgumentException.class,()->new PanelPosition(Double.NaN,0,false));
        assertThrows(IllegalArgumentException.class,()->new PanelPosition(0,Double.POSITIVE_INFINITY,false));
    }
    @Test void positionsAndFoldStatePersistAcrossConfigRestartAndProfiles() throws Exception {
        var registry=new ModuleRegistry(); var globals=new GlobalSettings();
        var config=new ConfigManager(directory.resolve("active.json"),registry,globals);
        globals.setPanelPosition(Category.WORLD,new PanelPosition(.75,.3,true));
        globals.setPanelPosition(Category.COMBAT,new PanelPosition(.1,.9,false));
        config.save(); var saved=config.snapshot();
        var restored=new GlobalSettings();
        var next=new ConfigManager(config.path(),registry,restored);
        assertTrue(next.load().warnings().isEmpty()); assertEquals(saved,next.snapshot());
        var profiles=new ProfileManager(directory.resolve("profiles"),registry,restored);
        profiles.save("Layout"); restored.clearPanelPositions();
        assertTrue(profiles.load("Layout").warnings().isEmpty()); assertEquals(saved,next.snapshot());
        profiles.restoreDefaults(); assertTrue(restored.panelPositions().isEmpty());
    }
    @Test void oldConfigsUseAutomaticPlacementAndMalformedPanelDoesNotLoseGoodNeighbors() {
        var globals=new GlobalSettings(); var config=new ConfigManager(directory.resolve("active.json"),new ModuleRegistry(),globals);
        globals.setPanelPosition(Category.WORLD,new PanelPosition(.5,.5,false));
        assertTrue(config.apply(JsonParser.parseString("{\"schemaVersion\":1,\"gui\":{\"category\":\"WORLD\"}}").getAsJsonObject()).isEmpty());
        assertTrue(globals.panelPositions().isEmpty());
        var result=config.apply(JsonParser.parseString("""
                {"gui":{"panels":{
                  "WORLD":{"x":0.6,"y":0.2,"collapsed":true},
                  "HUD":{"x":"bad","y":0.2,"collapsed":false},
                  "COMBAT":{"x":4,"y":-1,"collapsed":false},
                  "UNKNOWN":{"x":0.2,"y":0.2,"collapsed":false}}}}
                """).getAsJsonObject());
        assertEquals(1,result.size()); assertTrue(result.getFirst().contains("HUD"));
        assertEquals(new PanelPosition(.6,.2,true),globals.panelPositions().get(Category.WORLD));
        assertEquals(new PanelPosition(1,0,false),globals.panelPositions().get(Category.COMBAT));
        assertFalse(globals.panelPositions().containsKey(Category.HUD));
    }
    @Test void disablingGuiMemoryOmitsAndDoesNotLoadPanelState() {
        var globals=new GlobalSettings(); var config=new ConfigManager(directory.resolve("active.json"),new ModuleRegistry(),globals);
        globals.rememberGui.set(false); globals.setPanelPosition(Category.HUD,new PanelPosition(.5,.5,true));
        assertFalse(config.snapshot().has("gui"));
        assertTrue(config.apply(JsonParser.parseString("""
                {"global":{"remember_gui":false},"gui":{"panels":{"HUD":{"x":0.4,"y":0.3,"collapsed":true}}}}
                """).getAsJsonObject()).isEmpty());
        assertTrue(globals.panelPositions().isEmpty());
    }
}
