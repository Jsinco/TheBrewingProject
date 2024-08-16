package dev.jsinco.brewery.recipes;

import org.bukkit.Color;

import java.util.List;

/**
 * Encapsulation of a default recipe used when no recipe is found while brewing
 */
public record DefaultRecipe(String name, List<String> lore, Color color, int customModelData, boolean glint) {

}
