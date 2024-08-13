package dev.jsinco.brewery.recipes;

import dev.jsinco.brewery.enums.PotionQuality;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public record RecipeEffect(PotionEffectType effect, Map<PotionQuality, Integer> amplifier, Map<PotionQuality, Integer> durations) {
}
