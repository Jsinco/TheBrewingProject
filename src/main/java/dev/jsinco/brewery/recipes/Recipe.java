package dev.jsinco.brewery.recipes;

import dev.jsinco.brewery.enums.BarrelType;
import dev.jsinco.brewery.enums.PotionQuality;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Full recipe object used for creating a potion based off of a recipe.
 */
@Getter
public class Recipe {

    private final List<Ingredient> ingredients;
    private final Map<PotionQuality, String> names;
    private final Map<PotionQuality, List<String>> lore;
    private final Map<PotionQuality, List<String>> commands;
    private final Map<PotionQuality, RecipeEffect> effects;

    private final String color;
    private final int cookingTime;
    private final int distillRuns;
    private final int distillTime;
    private final BarrelType barrelType;
    private final int agingTime;
    private final int alcoholContent;
    private final boolean glint;

    public Recipe(List<Ingredient> ingredients, Map<PotionQuality, String> names, Map<PotionQuality, List<String>> lore, Map<PotionQuality, List<String>> commands, Map<PotionQuality, RecipeEffect> effects, String color, int cookingTime, int distillRuns, int distillTime, BarrelType barrelType, int agingTime, int alcoholContent, boolean glint) {
        this.ingredients = ingredients;
        this.names = names;
        this.lore = lore;
        this.commands = commands;
        this.effects = effects;
        this.color = color;
        this.cookingTime = cookingTime;
        this.distillRuns = distillRuns;
        this.distillTime = distillTime;
        this.barrelType = barrelType;
        this.agingTime = agingTime;
        this.alcoholContent = alcoholContent;
        this.glint = glint;
    }
}
