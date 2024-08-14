package dev.jsinco.brewery.recipes.ingredients;

import dev.jsinco.brewery.recipes.ingredients.integration.PluginIngredientMatcher;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class PluginIngredient implements Ingredient {

    private static final List<PluginIngredientMatcher> pluginMatchers = new ArrayList<>();

    private final String pluginName;
    private final String itemId;

    public PluginIngredient(String pluginName, String itemId) {
        this.pluginName = pluginName;
        this.itemId = itemId;
    }

    @Nullable
    public static PluginIngredient get(ItemStack item) {
        for (PluginIngredientMatcher pluginMatcher : pluginMatchers) {
            String itemId = pluginMatcher.getPluginItemId(item);
            if (itemId != null) {
                return new PluginIngredient(pluginMatcher.getPluginName(), itemId);
            }
        }
        return null;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        for (PluginIngredientMatcher pluginMatcher : pluginMatchers) {
            if (pluginMatcher.getPluginItemId(itemStack) != null) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginIngredient that = (PluginIngredient) o;
        return Objects.equals(pluginName, that.pluginName) && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginName, itemId);
    }
}
