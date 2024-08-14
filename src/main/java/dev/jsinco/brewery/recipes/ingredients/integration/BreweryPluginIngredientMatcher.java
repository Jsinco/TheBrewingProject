package dev.jsinco.brewery.recipes.ingredients.integration;

import org.bukkit.inventory.ItemStack;

public class BreweryPluginIngredientMatcher implements PluginIngredientMatcher {

    @Override
    public String getPluginName() {
        return "Brewery";
    }

    @Override
    public String getPluginItemId(ItemStack item) {
        // handle logic
        return "";
    }
}
