package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Locale;

/** Colors are packed ARGB in memory and #AARRGGBB in the configuration. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String id, String name, String description, int initialValue) {
        super(id, name, description);
        initialize(initialValue);
    }
    @Override protected Integer validate(Integer value) { return value; }
    public String hex() { return String.format(Locale.ROOT, "#%08X", get()); }
    public void setHex(String text) {
        if (text == null || !text.matches("#[0-9a-fA-F]{8}")) {
            throw new IllegalArgumentException("Use #AARRGGBB (including alpha)");
        }
        set((int) Long.parseLong(text.substring(1), 16));
    }
    @Override public JsonElement toJson() { return new JsonPrimitive(hex()); }
    @Override public void fromJson(JsonElement json) { setHex(readString(json)); }
}
