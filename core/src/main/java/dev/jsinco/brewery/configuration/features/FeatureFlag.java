package dev.jsinco.brewery.configuration.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum FeatureFlag {
    RESOURCE_PACK_COLORS,
    BREW_MAKING,
    BARRELS(BREW_MAKING),
    CAULDRONS(BREW_MAKING),
    DISTILLERIES(BREW_MAKING),
    DISPOSE_BREW,
    SEALING(BREW_MAKING),
    COLORED_CAULDRONS,
    BREW_DRINKING,
    BREW_EFFECTS,
    MODIFIER_CHANGE;

    private final List<FeatureFlag> thisAndParents;

    FeatureFlag(FeatureFlag... parentFlags) {
        List<FeatureFlag> featureFlags = new ArrayList<>();
        featureFlags.add(this);
        featureFlags.addAll(List.of(parentFlags));
        thisAndParents = Collections.unmodifiableList(featureFlags);
    }

    public List<FeatureFlag> thisAndParents() {
        return thisAndParents;
    }
}
