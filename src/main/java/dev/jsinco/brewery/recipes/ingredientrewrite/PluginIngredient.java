package dev.jsinco.brewery.recipes.ingredientrewrite;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// TODO: cleanup a little bit

/**
 * An ingredient based off of an external plugin ItemStack or local custom item/ingredient
 * defined in 'ingredients.yml'.
 */
@Getter
public abstract class PluginIngredient extends Ingredient {

    private static final Map<String, Supplier<PluginIngredient>> supportedPlugins = new HashMap<>();

    private String plugin;
    private String itemId;


    public abstract String getItemIdByItemStack(ItemStack itemStack);


    public static void registerPluginIngredient(String plugin, Supplier<PluginIngredient> supplier) {
        supportedPlugins.put(plugin.toUpperCase(), supplier);
    }

    /**
     * Attempt to get a PluginIngredient from an ItemStack.
     * @param itemStack The ItemStack to get the PluginIngredient from.
     * @return The PluginIngredient if the ItemStack is a valid PluginIngredient, otherwise null.
     */
    public static PluginIngredient of(ItemStack itemStack) {
        for (Map.Entry<String, Supplier<PluginIngredient>> entry : supportedPlugins.entrySet()) {
            PluginIngredient pluginIngredient = entry.getValue().get();
            if (pluginIngredient.matches(itemStack)) {
                pluginIngredient.plugin = entry.getKey();
                pluginIngredient.itemId = pluginIngredient.getItemIdByItemStack(itemStack);
                pluginIngredient.amount = itemStack.getAmount();
                return pluginIngredient;
            }
        }
        return null;
    }

    /**
     * Attempt to get a PluginIngredient from a plugin name and item id.
     * @param plugin The plugin name.
     * @param itemId The item id.
     * @param amount The amount of the item.
     * @return The PluginIngredient if the plugin is supported, otherwise null.
     */
    public static PluginIngredient of(String plugin, String itemId, int amount) {
        Supplier<PluginIngredient> supplier = supportedPlugins.get(plugin.toUpperCase());
        if (supplier != null) {
            PluginIngredient pluginIngredient = supplier.get();
            pluginIngredient.plugin = plugin;
            pluginIngredient.itemId = itemId;
            pluginIngredient.amount = amount;
            return pluginIngredient;
        }
        return null;
    }
}
