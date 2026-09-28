package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;
    public DoubleSetting(String id, String name, String description, double initialValue, double min, double max) {
        super(id, name, description);
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Invalid bounds");
        }
        this.min = min;
        this.max = max;
        initialize(initialValue);
    }
    public double min() { return min; }
    public double max() { return max; }
    @Override protected Double validate(Double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Value must be finite");
        return Math.clamp(value, min, max);
    }
    @Override public JsonElement toJson() { return new JsonPrimitive(get()); }
    @Override public void fromJson(JsonElement json) { set(readNumber(json)); }
}
