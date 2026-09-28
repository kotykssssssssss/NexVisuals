package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;

public final class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;
    public IntSetting(String id, String name, String description, int initialValue, int min, int max) {
        super(id, name, description);
        if (min > max) throw new IllegalArgumentException("Inverted bounds");
        this.min = min;
        this.max = max;
        initialize(initialValue);
    }
    public int min() { return min; }
    public int max() { return max; }
    @Override protected Integer validate(Integer value) { return Math.clamp(value, min, max); }
    @Override public JsonElement toJson() { return new JsonPrimitive(get()); }
    @Override public void fromJson(JsonElement json) {
        requirePrimitive(json);
        if (!json.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected an integer");
        BigDecimal value = json.getAsBigDecimal();
        if (value.stripTrailingZeros().scale() > 0) throw new IllegalArgumentException("Expected an integer");
        set(value.max(BigDecimal.valueOf(min)).min(BigDecimal.valueOf(max)).intValueExact());
    }
}
