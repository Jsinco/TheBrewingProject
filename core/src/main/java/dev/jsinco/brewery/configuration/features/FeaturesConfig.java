package dev.jsinco.brewery.configuration.features;

import dev.jsinco.brewery.api.util.Logger;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Exclude;
import eu.okaeri.configs.annotation.Header;
import eu.okaeri.configs.serdes.OkaeriSerdes;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Set;

@Header("Specify if you want to disable or enable features globally or in a world")
public class FeaturesConfig extends OkaeriConfig {
    @Comment("Whitelist or blacklist features")
    @CustomKey("global")
    private FeaturePredicate global = new FeaturePredicate(Set.of(), false);

    @Comment("Specify the world to whitelist or blacklist features")
    @CustomKey("worlds")
    private Map<String, FeaturePredicate> worlds = Map.of("world", new FeaturePredicate(Set.of(), false));

    @Exclude
    private static FeaturesConfig instance;

    public static boolean test(FeatureFlag featureFlag, @Nullable String worldName) {
        if (instance == null) {
            Logger.logErr("Features config not initialized! - This is a bug that will disable all features");
            return false;
        }
        if (worldName != null && instance.worlds.get(worldName) instanceof FeaturePredicate featurePredicate) {
            return featurePredicate.test(featureFlag);
        }
        return instance.global.test(featureFlag);
    }

    public static void load(File dataFolder, OkaeriSerdes... packs) {
        FeaturesConfig.instance = ConfigManager.create(FeaturesConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer(), packs);
            it.withBindFile(new File(dataFolder, "features.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });
    }

    public static void reload() {
        instance.load(true);
    }
}
