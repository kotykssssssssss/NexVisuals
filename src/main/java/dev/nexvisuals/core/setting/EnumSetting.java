package dev.nexvisuals.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Objects;

public final class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final Class<E> enumType;
    private final List<E> values;
    public EnumSetting(String id, String name, String description, E initialValue, Class<E> enumType) {
        super(id, name, description);
        this.enumType = Objects.requireNonNull(enumType);
        this.values = List.of(enumType.getEnumConstants());
        initialize(initialValue);
    }
    public List<E> values() { return values; }
    public void cycle() { set(values.get((values.indexOf(get()) + 1) % values.size())); }
    @Override protected E validate(E value) {
        if (value.getDeclaringClass() != enumType) throw new IllegalArgumentException("Unexpected enum type");
        return value;
    }
    @Override public JsonElement toJson() { return new JsonPrimitive(get().name()); }
    @Override public void fromJson(JsonElement json) { set(Enum.valueOf(enumType, readString(json))); }
}
