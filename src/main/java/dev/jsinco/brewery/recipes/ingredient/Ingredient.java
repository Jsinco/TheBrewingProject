package dev.jsinco.brewery.recipes.ingredient;

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
     * Overriden equals implementation to ensure ingredients properly match each other when compared
     * @param o The Ingredient class we're comparing this class to
     * @return if the objects equal each other or not
     */
    @Override
    public abstract boolean equals(Object o);

    /**
     * Check if the itemStack matches the ingredient amount and ItemStack.
     * @param itemStack The itemStack to check.
     * @return True if the itemStack matches the ingredient exactly, false otherwise.
     */
    public boolean matchesExact(ItemStack itemStack) {
        return itemStack.getAmount() == amount && matches(itemStack);
    }
}
