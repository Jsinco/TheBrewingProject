package dev.jsinco.brewery.recipes.ingredientrewrite;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an ingredient in a recipe.
 */
@Setter @Getter
public abstract class Ingredient {

    protected int amount = 1;

    /**
     * Check if the itemStack matches the ingredient.
     * @param itemStack The itemStack to check.
     * @return True if the itemStack matches the ingredient, false otherwise.
     */
    public abstract boolean matches(ItemStack itemStack);

    /**
     * Check if the itemStack matches the ingredient amount and ItemStack.
     * @param itemStack The itemStack to check.
     * @return True if the itemStack matches the ingredient exactly, false otherwise.
     */
    public boolean matchesExact(ItemStack itemStack) {
        return itemStack.getAmount() == amount && matches(itemStack);
    }
}
