package dev.jsinco.brewery.recipes.ingredients;

import org.bukkit.inventory.ItemStack;

/**
 * Interface used to determine if an item stack matches an ingredient.
 */
public interface Ingredient {

    boolean matches(ItemStack itemStack);

    @Override
    boolean equals(Object obj);

    static Ingredient getIngredient(ItemStack itemStack) {
        Ingredient ingredient = CustomIngredient.get(itemStack); // First try to get a custom ingredient
        if (ingredient == null) {
            ingredient = PluginIngredient.get(itemStack); // Then try to get a plugin ingredient
        }
        if (ingredient == null) {
            ingredient = SimpleIngredient.get(itemStack); // Finally, default to a simple ingredient
        }
        return ingredient;
    }


}
