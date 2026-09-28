package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** An ordered pair with both ends clamped to a common allowed interval. */
public final class RangeSetting extends Setting<DoubleRange> {
    private final double min;
    private final double max;
    public RangeSetting(String id, String name, String description, DoubleRange initialValue, double min, double max) {
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
    @Override protected DoubleRange validate(DoubleRange value) {
        double first = Math.clamp(value.lower(), min, max);
        double second = Math.clamp(value.upper(), min, max);
        return new DoubleRange(Math.min(first, second), Math.max(first, second));
    }
    @Override public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("lower", get().lower());
        json.addProperty("upper", get().upper());
        return json;
    }
    @Override public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonObject()) throw new IllegalArgumentException("Expected a range object");
        JsonObject object = json.getAsJsonObject();
        set(new DoubleRange(readNumber(object.get("lower")), readNumber(object.get("upper"))));
    }
}
