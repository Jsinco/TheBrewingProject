package dev.jsinco.brewery;

import dev.jsinco.brewery.factories.RecipeFactory;
import dev.jsinco.brewery.recipes.ingredient.external.OraxenPluginIngredient;
import dev.jsinco.brewery.recipes.ingredient.PluginIngredient;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class TheBrewingProject extends JavaPlugin {

    @Getter
    private static TheBrewingProject instance;
    @Getter
    private static RecipeFactory recipeFactory;

    @Override
    public void onLoad() {
        instance = this;
        recipeFactory = new RecipeFactory();
    }

    public void registerPluginIngredients() {
        PluginIngredient.registerPluginIngredient("Oraxen", OraxenPluginIngredient::new);
    }
}