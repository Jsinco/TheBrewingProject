package dev.jsinco.brewery.recipes.ingredientrewrite.external;

import dev.jsinco.brewery.recipes.ingredientrewrite.PluginIngredient;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.inventory.ItemStack;

// PluginIngredient usage example using Oraxen
public class OraxenPluginIngredient extends PluginIngredient {
    @Override
    public boolean matches(ItemStack itemStack) {
        return this.getItemIdByItemStack(itemStack) != null;
    }

    @Override
    public String getItemIdByItemStack(ItemStack itemStack) {
        return OraxenItems.getIdByItem(itemStack);
    }
}
