package dev.jsinco.brewery.recipes.ingredients;

import dev.jsinco.brewery.objects.ObjectManager;
import org.bukkit.inventory.ItemStack;

/**
 * Interface used to determine if an item stack matches an ingredient.
 */
public interface Ingredient {

    @Override
    boolean equals(Object obj);

    // FIXME - Move this
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

    static Ingredient getIngredient(String ingredientString) {
        if (!ingredientString.contains(":")) {
            return SimpleIngredient.of(ingredientString);
        }
        String[] split = ingredientString.split(":");
        if (split[0].equalsIgnoreCase("custom")) {
            for (CustomIngredient customIngredient : ObjectManager.getCustomIngredients()) {
                if (customIngredient.getId().equals(split[1])) {
                    return customIngredient;
                }
            }
        } else {
            return PluginIngredient.of(split[0], split[1]);
        }
    }


}
