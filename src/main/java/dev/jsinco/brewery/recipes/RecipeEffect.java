package dev.jsinco.brewery.recipes;

import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.util.Pair;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

// FIXME - needs to convert from seconds to minecraft ticks
public record RecipeEffect(PotionEffectType effect, Pair<Integer, Integer> durationBounds, Pair<Integer, Integer> amplifierBounds) {

    public static RecipeEffect of(PotionEffectType effect, Pair<Integer, Integer> durationBounds, Pair<Integer, Integer> amplifierBounds) {
        return new RecipeEffect(effect, durationBounds, amplifierBounds);
    }

    public int getAmplifierBasedOnQuality(PotionQuality quality) {
        if (!amplifiers.containsKey(quality)) {
            return amplifiers.values().iterator().next();
        }
        return amplifiers.get(quality);
    }


    public int getDurationBasedOnQuality(PotionQuality quality) {
        if (!durations.containsKey(quality)) {
            return durations.values().iterator().next();
        }
        return durations.get(quality);
    }

    public PotionEffect getPotionEffect(PotionQuality quality) {
        return new PotionEffect(effect, this.getDurationBasedOnQuality(quality), this.getAmplifierBasedOnQuality(quality));
    }
}
