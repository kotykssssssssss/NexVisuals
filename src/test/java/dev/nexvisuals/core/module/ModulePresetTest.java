package dev.nexvisuals.core.module;

import dev.nexvisuals.core.setting.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModulePresetTest {
    static final class Example extends VisualModule {
        final IntSetting count=add(new IntSetting("count","Count","",4,0,10));
        final ColorSetting color=add(new ColorSetting("color","Color","",0xFF123456));
        Example() { super("example","Example","",Category.GENERAL); preset("Good","","count",8); preset("Broken","","count",2,"color","invalid"); }
    }
    @Test void styleResetsOmittedValuesAndKeepsEnableState() {
        Example m=new Example(); m.setEnabled(true); m.color.set(0xAA987654);
        m.applyPreset(m.presets().getFirst()); assertEquals(8,m.count.get()); assertEquals(0xFF123456,m.color.get()); assertTrue(m.enabled());
    }
    @Test void invalidStyleRollsBackAllValuesAndForeignStylesAreRejected() {
        Example m=new Example(); m.count.set(9); m.color.set(0xCC778899);
        assertThrows(IllegalArgumentException.class,()->m.applyPreset(m.presets().get(1)));
        assertEquals(9,m.count.get()); assertEquals(0xCC778899,m.color.get());
        assertThrows(IllegalArgumentException.class,()->m.applyPreset(new ModulePreset("Foreign","",java.util.Map.of())));
    }
}
