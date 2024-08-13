package dev.jsinco.brewery.recipes;

import org.bukkit.inventory.ItemStack;

public interface Ingredient {
    boolean matches(ItemStack itemStack);
}
