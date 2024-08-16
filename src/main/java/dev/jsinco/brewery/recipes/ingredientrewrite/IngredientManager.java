package dev.jsinco.brewery.recipes.ingredientrewrite;

import dev.jsinco.brewery.util.Util;
import org.bukkit.inventory.ItemStack;

/**
 * Get an instance of an ingredient from an ItemStack or a string.
 * Used for cauldrons and loading for loading recipes.
 */
public class IngredientManager {


    public static Ingredient getIngredient(ItemStack itemStack) {
        Ingredient ingredient = PluginIngredient.of(itemStack);
        if (ingredient == null) {
            ingredient = SimpleIngredient.of(itemStack);
        }
        return ingredient;
    }


    public static Ingredient getIngredient(String ingredientStr) {
        Ingredient ingredient = null;
        String[] p1 = ingredientStr.split("/");
        String str = p1[0];
        int amount = Util.getInt(p1[1]);

        if (str.contains(":")) {
            String[] p2 = str.split(":");
            String type = p2[0];
            String itemId = p2[1];


            ingredient = PluginIngredient.of(type, itemId, amount);
        }

        if (ingredient == null) {
            ingredient = SimpleIngredient.of(str, amount);
        }
        return ingredient;
    }
}
