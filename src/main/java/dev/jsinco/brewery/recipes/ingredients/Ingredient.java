package dev.jsinco.brewery.recipes.ingredients;

import org.bukkit.inventory.ItemStack;

/**
 * Interface used to determine if an item stack matches an ingredient.
 */
public interface Ingredient {
    boolean matches(ItemStack itemStack);
}
