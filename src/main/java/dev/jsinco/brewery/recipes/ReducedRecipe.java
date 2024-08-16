package dev.jsinco.brewery.recipes;

import dev.jsinco.brewery.enums.BarrelType;
import dev.jsinco.brewery.enums.CauldronType;
import dev.jsinco.brewery.files.Config;
import dev.jsinco.brewery.recipes.ingredients.Ingredient;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.List;

/**
 * Recipe object with fewer attributes only used for identifying which recipe is being brewed
 */
@Getter
public class ReducedRecipe {

    protected final String recipeName;

    // Used for identifying and for particle effects while brewing in a cauldron
    protected final List<Ingredient> ingredients;
    protected final int brewTime;
    protected final Color color;
    protected final int brewDifficulty;
    protected final CauldronType cauldronType;
    protected final List<Material> heatSources;

    // Used for aging
    protected final BarrelType barrelType;
    protected final int agingYears;

    // Used for distilling
    protected final int distillRuns;
    protected final int distillTime;


    public ReducedRecipe(String recipeName, List<Ingredient> ingredients, int brewTime, Color color, int brewDifficulty, CauldronType cauldronType, List<Material> heatSources, BarrelType barrelType, int agingYears, int distillRuns, int distillTime) {
        this.recipeName = recipeName;
        this.ingredients = ingredients;
        this.brewTime = brewTime;
        this.color = color == null ? Color.BLUE : color;
        this.brewDifficulty = brewDifficulty;
        this.cauldronType = cauldronType == null ? CauldronType.WATER : cauldronType;
        this.heatSources = heatSources == null ? Config.HEAT_SOURCES : heatSources;
        this.barrelType = barrelType == null ? BarrelType.OAK : barrelType;
        this.agingYears = agingYears;
        this.distillRuns = distillRuns;
        this.distillTime = distillTime;
    }

}
