package dev.jsinco.brewery.recipes.ingredients.integration;

import org.bukkit.inventory.ItemStack;

public interface PluginIngredientMatcher {

    String getPluginName();

    String getPluginItemId(ItemStack item);
}
