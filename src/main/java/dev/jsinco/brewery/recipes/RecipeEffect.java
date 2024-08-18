package dev.jsinco.brewery.recipes;

import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.util.Pair;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Random;

// FIXME - needs to convert from seconds to minecraft ticks
public record RecipeEffect(PotionEffectType effect, Pair<Integer, Integer> durationBounds, Pair<Integer, Integer> amplifierBounds) {

    private static final Random RANDOM = new Random();

    public static RecipeEffect of(PotionEffectType effect, Pair<Integer, Integer> durationBounds, Pair<Integer, Integer> amplifierBounds) {
        return new RecipeEffect(effect, durationBounds, amplifierBounds);
    }

    public PotionEffect getPotionEffect(PotionQuality quality) {
        return switch (quality) {
            // Return the lowest (first) bound
            case BAD -> new PotionEffect(effect, durationBounds.first(), amplifierBounds.first());
            // Return a value between the first and second bounds
            case GOOD -> new PotionEffect(effect, RANDOM.nextInt(durationBounds.first(), durationBounds.second()), RANDOM.nextInt(amplifierBounds.first(), amplifierBounds.second()));
            // Return the highest (second) bound
            case EXCELLENT -> new PotionEffect(effect, durationBounds.second(), amplifierBounds.second());
        };
    }
}
