package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, String name, String description, boolean initialValue) {
        super(id, name, description);
        initialize(initialValue);
    }
    @Override protected Boolean validate(Boolean value) { return value; }
    @Override public JsonElement toJson() { return new JsonPrimitive(get()); }
    @Override public void fromJson(JsonElement json) {
        requirePrimitive(json);
        if (!json.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected boolean");
        set(json.getAsBoolean());
    }
}
