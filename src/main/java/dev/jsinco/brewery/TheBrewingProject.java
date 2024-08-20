package dev.jsinco.brewery;

import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import dev.jsinco.brewery.factories.RecipeFactory;
import dev.jsinco.brewery.listeners.BreweryEvents;
import dev.jsinco.brewery.objects.Tickable;
import dev.jsinco.brewery.recipes.ingredient.custom.CustomIngredientManager;
import dev.jsinco.brewery.recipes.ingredient.external.OraxenPluginIngredient;
import dev.jsinco.brewery.recipes.ingredient.PluginIngredient;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;

public class TheBrewingProject extends JavaPlugin {

    @Getter
    private static TheBrewingProject instance;
    @Getter
    private static TaskScheduler scheduler;
    @Getter @Setter
    private static RecipeFactory recipeFactory;

    @Override
    public void onLoad() {
        instance = this;
        scheduler = UniversalScheduler.getScheduler(this);
        recipeFactory = new RecipeFactory();
    }

    @Override
    public void onEnable() {
        CustomIngredientManager.reloadCustomIngredients();
        this.registerPluginIngredients();



        this.getServer().getPluginManager().registerEvents(new BreweryEvents(), this);

        // Start ticking objects
        scheduler.runTaskTimerAsynchronously(() -> {
            for (Tickable tickable : Tickable.getActiveCauldrons()) {
                tickable.asyncFastTick();
            }
        }, 0L, 1L);
        scheduler.runTaskTimer(() -> {
            for (Tickable tickable : Tickable.getActiveCauldrons()) {
                tickable.tick();
            }
            for (Tickable tickable : Tickable.getActiveBarrels()) {
                tickable.tick();
            }
            for (Tickable tickable : Tickable.getActiveBreweryPlayers()) {
                tickable.tick();
            }
        }, 0L, 1200L);
    }

    public void registerPluginIngredients() {
        PluginIngredient.registerPluginIngredient("Custom", CustomIngredientManager::new, false);
        PluginIngredient.registerPluginIngredient("Oraxen", OraxenPluginIngredient::new, true);
    }
}