package dev.jsinco.brewery.configuration.features;

import java.util.Set;

public record FeaturePredicate(Set<FeatureFlag> featureFlags, boolean whiteList) {

    /**
     *
     * @param featureFlag A feature flag
     * @return True if the feature matches
     */
    public boolean test(FeatureFlag featureFlag) {
        return whiteList == featureFlag.thisAndParents()
                .stream()
                .anyMatch(featureFlags::contains);
    }
}
