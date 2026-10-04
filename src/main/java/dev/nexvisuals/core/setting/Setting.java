package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A module declares settings once; persistence and the GUI use the same definitions. */
public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final String description;
    private T defaultValue;
    private T value;
    private long revision;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String id, String name, String description) {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid setting ID: " + id);
        }
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNullElse(description, "");
    }

    // Called by subclasses after their validation bounds have been initialized.
    protected final void initialize(T initialValue) {
        defaultValue = validate(Objects.requireNonNull(initialValue));
        value = defaultValue;
    }

    public final String id() { return id; }
    public final String name() { return name; }
    public final String description() { return description; }
    public final T defaultValue() { return defaultValue; }
    public final T get() { return value; }
    public final long revision() { return revision; }
    public final boolean visible() { return visibility.getAsBoolean(); }
    public final void visibleWhen(BooleanSupplier condition) { visibility = Objects.requireNonNull(condition); }
    public final void set(T value) {
        T checked = validate(Objects.requireNonNull(value));
        if (!Objects.equals(this.value, checked)) { this.value = checked; revision++; }
    }
    public final void reset() { set(defaultValue); }

    protected abstract T validate(T value);
    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement json);

    protected static void requirePrimitive(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) {
            throw new IllegalArgumentException("Expected a primitive setting value");
        }
    }

    protected static String readString(JsonElement json) {
        requirePrimitive(json);
        if (!json.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected text");
        return json.getAsString();
    }

    protected static double readNumber(JsonElement json) {
        requirePrimitive(json);
        if (!json.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected a number");
        double number = json.getAsDouble();
        if (!Double.isFinite(number)) throw new IllegalArgumentException("Expected a finite number");
        return number;
    }
}
