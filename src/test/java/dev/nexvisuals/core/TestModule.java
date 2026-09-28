package dev.nexvisuals.core;

import dev.nexvisuals.core.module.Category;
import dev.nexvisuals.core.module.VisualModule;
import dev.nexvisuals.core.setting.*;

public final class TestModule extends VisualModule {
    public enum Style { SIMPLE, COMPACT }
    public final BooleanSetting flag = add(new BooleanSetting("flag", "Flag", "", true));
    public final IntSetting count = add(new IntSetting("count", "Count", "", 4, 1, 10));
    public final DoubleSetting size = add(new DoubleSetting("size", "Size", "", 1, 0.1, 4));
    public final RangeSetting range = add(new RangeSetting("range", "Range", "", new DoubleRange(2, 8), 0, 10));
    public final EnumSetting<Style> style = add(new EnumSetting<>("style", "Style", "", Style.SIMPLE, Style.class));
    public final ColorSetting color = add(new ColorSetting("color", "Color", "", 0xFFABCDEF));
    public final TextSetting text = add(new TextSetting("text", "Text", "", "Default", 12));
    public final KeybindSetting key = add(new KeybindSetting("key", "Key", "", "key.keyboard.k"));
    public int enabledCalls;
    public int disabledCalls;

    public TestModule(String id) { super(id, "Test", "Fixture module", Category.HUD); }
    public <S extends Setting<?>> S declare(S setting) { return add(setting); }
    @Override protected void onEnable() { enabledCalls++; }
    @Override protected void onDisable() { disabledCalls++; }
}
