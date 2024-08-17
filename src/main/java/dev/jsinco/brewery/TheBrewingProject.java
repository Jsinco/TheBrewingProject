package dev.jsinco.brewery;

import dev.jsinco.brewery.recipes.ingredient.external.OraxenPluginIngredient;
import dev.jsinco.brewery.recipes.ingredient.PluginIngredient;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class TheBrewingProject extends JavaPlugin {

    @Getter
    private static TheBrewingProject instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    public void registerPluginIngredients() {
        PluginIngredient.registerPluginIngredient("Oraxen", OraxenPluginIngredient::new);
    }
}