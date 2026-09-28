package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Stores a Minecraft input translation key; client code handles actual key events. */
public final class KeybindSetting extends Setting<String> {
    public KeybindSetting(String id, String name, String description, String initialValue) {
        super(id, name, description);
        initialize(initialValue);
    }
    @Override protected String validate(String value) {
        if (value.length() > 96 || !value.matches("key\\.(keyboard|mouse)\\.[a-z0-9.]+")) {
            throw new IllegalArgumentException("Expected an input key such as key.keyboard.right.shift");
        }
        return value;
    }
    @Override public JsonElement toJson() { return new JsonPrimitive(get()); }
    @Override public void fromJson(JsonElement json) { set(readString(json)); }
}
