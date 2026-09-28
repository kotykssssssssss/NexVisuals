package dev.nexvisuals.core.setting;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.nexvisuals.core.TestModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingTest {
    @Test void integerClampCoversDirectAndDeserializedInputWithoutOverflow() {
        IntSetting setting = new IntSetting("number", "Number", "Description", 4, 1, 10);
        setting.set(-100);
        assertEquals(1, setting.get());
        setting.set(100);
        assertEquals(10, setting.get());
        setting.fromJson(JsonParser.parseString("999999999999999999999999999999999"));
        assertEquals(10, setting.get());
        assertThrows(IllegalArgumentException.class, () -> setting.fromJson(new JsonPrimitive(3.5)));
        setting.reset();
        assertEquals(4, setting.get());
        assertEquals("Description", setting.description());
    }

    @Test void doubleClampRejectsNonFiniteValues() {
        DoubleSetting setting = new DoubleSetting("size", "Size", null, 1, 0.1, 4);
        setting.set(-5.0);
        assertEquals(0.1, setting.get());
        setting.set(100.0);
        assertEquals(4, setting.get());
        assertThrows(IllegalArgumentException.class, () -> setting.set(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> setting.fromJson(new JsonPrimitive(Double.POSITIVE_INFINITY)));
        assertEquals(4, setting.get());
    }

    @Test void invalidBoundsAreRejectedAndDefaultsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new IntSetting("x", "X", "", 0, 10, 2));
        assertThrows(IllegalArgumentException.class, () -> new DoubleSetting("x", "X", "", 0, 0, Double.NaN));
        assertEquals(10, new IntSetting("x", "X", "", 100, 0, 10).defaultValue());
    }

    @Test void rangesClampAndOrderEndpointsAtomically() {
        RangeSetting range = new RangeSetting("range", "Range", "", new DoubleRange(2, 8), 0, 10);
        range.set(new DoubleRange(100, -5));
        assertEquals(new DoubleRange(0, 10), range.get());
        range.fromJson(JsonParser.parseString("{\"lower\":8,\"upper\":3}"));
        assertEquals(new DoubleRange(3, 8), range.get());
        assertThrows(IllegalArgumentException.class, () -> range.fromJson(JsonParser.parseString("{\"lower\":1}")));
        assertEquals(new DoubleRange(3, 8), range.get());
    }

    @Test void booleanAndEnumValuesAreTypedAndUnknownEnumsRejected() {
        BooleanSetting flag = new BooleanSetting("flag", "Flag", "", false);
        assertThrows(IllegalArgumentException.class, () -> flag.fromJson(new JsonPrimitive("true")));
        EnumSetting<TestModule.Style> style = new EnumSetting<>("style", "Style", "", TestModule.Style.SIMPLE, TestModule.Style.class);
        style.cycle();
        assertEquals(TestModule.Style.COMPACT, style.get());
        style.cycle();
        assertEquals(TestModule.Style.SIMPLE, style.get());
        assertThrows(IllegalArgumentException.class, () -> style.fromJson(new JsonPrimitive("REMOVED")));
    }

    @Test void alphaColorRoundTripsWithoutSignLoss() {
        ColorSetting color = new ColorSetting("color", "Color", "", 0xFFABCDEF);
        assertEquals("#FFABCDEF", color.hex());
        color.setHex("#80aabbcc");
        assertEquals(0x80AABBCC, color.get());
        ColorSetting restored = new ColorSetting("color", "Color", "", 0);
        restored.fromJson(color.toJson());
        assertEquals(color.get(), restored.get());
        assertThrows(IllegalArgumentException.class, () -> color.setHex("#ABCDEF"));
    }

    @Test void textTruncationDoesNotSplitSurrogatePairsAndKeysAreValidated() {
        TextSetting text = new TextSetting("text", "Text", "", "", 2);
        text.set("A\uD83D\uDE00B");
        assertEquals("A\uD83D\uDE00", text.get());
        KeybindSetting key = new KeybindSetting("key", "Key", "", "key.keyboard.right.shift");
        key.set("key.mouse.left");
        assertThrows(IllegalArgumentException.class, () -> key.set("../../run.bat"));
        assertThrows(NullPointerException.class, () -> text.set(null));
    }
}
