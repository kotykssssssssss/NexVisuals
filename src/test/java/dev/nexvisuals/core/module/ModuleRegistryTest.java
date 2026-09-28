package dev.nexvisuals.core.module;

import dev.nexvisuals.core.TestModule;
import dev.nexvisuals.core.setting.BooleanSetting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleRegistryTest {
    @Test void preservesRegistrationOrderAndRejectsDuplicatesWithoutReplacement() {
        ModuleRegistry registry = new ModuleRegistry();
        TestModule first = registry.register(new TestModule("first"));
        TestModule second = registry.register(new TestModule("second"));
        assertEquals(java.util.List.of(first, second), registry.all());
        assertThrows(IllegalArgumentException.class, () -> registry.register(new TestModule("first")));
        assertSame(first, registry.find("first").orElseThrow());
        assertTrue(registry.find("missing").isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
    }

    @Test void rejectsInvalidIdsAndDuplicateSettings() {
        assertThrows(IllegalArgumentException.class, () -> new TestModule("invalid-id"));
        TestModule module = new TestModule("valid");
        int originalCount = module.settings().size();
        assertThrows(IllegalArgumentException.class, () -> module.declare(new BooleanSetting("flag", "Duplicate", "", false)));
        assertEquals(originalCount, module.settings().size());
        assertThrows(UnsupportedOperationException.class, () -> module.settings().clear());
    }

    @Test void lifecycleRunsOnlyOnActualTransitions() {
        TestModule module = new TestModule("test");
        module.setEnabled(false);
        module.setEnabled(true);
        module.setEnabled(true);
        assertTrue(module.enabled());
        module.toggle();
        assertFalse(module.enabled());
        assertEquals(1, module.enabledCalls);
        assertEquals(1, module.disabledCalls);
    }

    @Test void failedLifecycleDoesNotPublishEnabledState() {
        VisualModule module = new VisualModule("failure", "Failure", "", Category.GENERAL) {
            @Override protected void onEnable() { throw new IllegalStateException("Expected test failure"); }
        };
        assertThrows(IllegalStateException.class, () -> module.setEnabled(true));
        assertFalse(module.enabled());
    }
}
