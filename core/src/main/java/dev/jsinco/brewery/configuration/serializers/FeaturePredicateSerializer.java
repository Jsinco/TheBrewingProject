package dev.jsinco.brewery.configuration.serializers;

import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturePredicate;
import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@NullMarked
public class FeaturePredicateSerializer implements ObjectSerializer<FeaturePredicate> {
    @Override
    public boolean supports(Class<?> type) {
        return FeaturePredicate.class == type;
    }

    @Override
    public void serialize(@Nullable FeaturePredicate object, SerializationData data, GenericsDeclaration generics) {
        if (object == null) {
            return;
        }
        if (object.whiteList()) {
            data.setCollection("whitelist", object.featureFlags(), FeatureFlag.class);
        } else {
            data.setCollection("blacklist", object.featureFlags(), FeatureFlag.class);
        }
    }

    @Override
    public FeaturePredicate deserialize(DeserializationData data, GenericsDeclaration generics) {
        if (data.containsKey("whitelist")) {
            return new FeaturePredicate(data.getAsSet("whitelist", FeatureFlag.class), true);
        } else if (data.containsKey("blacklist")) {
            return new FeaturePredicate(data.getAsSet("blacklist", FeatureFlag.class), false);
        }
        return new FeaturePredicate(Set.of(), false);
    }
}
