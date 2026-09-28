package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class TextSetting extends Setting<String> {
    private final int maxLength;
    public TextSetting(String id, String name, String description, String initialValue, int maxLength) {
        super(id, name, description);
        if (maxLength < 0) throw new IllegalArgumentException("Negative maximum length");
        this.maxLength = maxLength;
        initialize(initialValue);
    }
    public int maxLength() { return maxLength; }
    @Override protected String validate(String value) {
        if (value.codePointCount(0, value.length()) <= maxLength) return value;
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
    @Override public JsonElement toJson() { return new JsonPrimitive(get()); }
    @Override public void fromJson(JsonElement json) { set(readString(json)); }
}
