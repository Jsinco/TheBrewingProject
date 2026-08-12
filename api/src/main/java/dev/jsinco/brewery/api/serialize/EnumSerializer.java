package dev.jsinco.brewery.api.serialize;

import java.util.Arrays;
import java.util.Locale;

public record EnumSerializer<E extends Enum<E>>(Class<E> eClass) implements Serializer<E> {
    @Override
    public E deserialize(String serialized) {
        return Arrays.stream(eClass.getEnumConstants())
                .filter(enumConstant -> enumConstant.name().equalsIgnoreCase(serialized))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String serializeSafely(E deserialized) {
        return deserialized.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean appliesTo(Object object) {
        return eClass.isInstance(object);
    }
}
