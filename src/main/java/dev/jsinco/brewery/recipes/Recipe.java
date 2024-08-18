package dev.jsinco.brewery.recipes;

import dev.jsinco.brewery.enums.BarrelType;
import dev.jsinco.brewery.enums.CauldronType;
import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.recipes.ingredient.Ingredient;
import lombok.Getter;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full recipe object used for creating a potion based off of a recipe.
 */
@Getter
public class Recipe extends ReducedRecipe {

    private final int alcohol;

    // Potion attributes
    private final Map<PotionQuality, String> names;
    private final Map<PotionQuality, List<String>> lore;
    private final boolean glint;

    // Commands
    private final Map<PotionQuality, List<String>> commands;
    // Effects
    private final List<RecipeEffect> effects;
    // Messages <-- Consider removing because server owners can use commands
    private final String title;
    private final String message;
    private final String actionBar;


    public Recipe(String recipeName, int brewTime, int brewDifficulty, int alcohol, CauldronType cauldronType, List<Ingredient> ingredients, Map<PotionQuality, String> names, Map<PotionQuality, List<String>> lore, Color color, boolean glint, int distillRuns, int distillTime, BarrelType barrelType, int agingYears, Map<PotionQuality, List<String>> commands, List<RecipeEffect> effects, String title, String message, String actionBar) {
        super(recipeName, ingredients, brewTime, color, brewDifficulty, cauldronType, barrelType, agingYears, distillRuns, distillTime);
        this.alcohol = alcohol;
        this.names = names;
        this.lore = lore;
        this.glint = glint;
        this.commands = commands;
        this.effects = effects;
        this.title = title;
        this.message = message;
        this.actionBar = actionBar;
    }

    public String getNameBasedOnQuality(PotionQuality quality) {
        if (!names.containsKey(quality)) {
            return names.values().iterator().next();
        }
        return names.get(quality);
    }

    public List<String> getLoreBasedOnQuality(PotionQuality quality) {
        if (!lore.containsKey(quality)) {
            return lore.values().iterator().next();
        }
        return lore.get(quality);
    }

    public List<String> getCommandsBasedOnQuality(PotionQuality quality) {
        if (!commands.containsKey(quality)) {
            return commands.values().iterator().next();
        }
        return commands.get(quality);
    }


    // create recipebuilder that does not extend anything
    public static class Builder {
        private final String recipeName;
        private List<Ingredient> ingredients = new ArrayList<>();
        private int brewTime = 1;
        private Color color = Color.BLUE;
        private int brewDifficulty = 1;
        private CauldronType cauldronType = CauldronType.WATER;
        private BarrelType barrelType = BarrelType.ANY;
        private int agingYears = 0;
        private int distillRuns = 0;
        private int distillTime = 0;
        private int alcohol = 0;
        private Map<PotionQuality, String> names = new HashMap<>();
        private Map<PotionQuality, List<String>> lore = new HashMap<>();
        private boolean glint = false;
        private Map<PotionQuality, List<String>> commands = new HashMap<>();
        private List<RecipeEffect> effects = new ArrayList<>();
        private String title = "";
        private String message = "";
        private String actionBar = "";

        public Builder(String recipeName) {
            this.recipeName = recipeName;
        }

        public Builder ingredients(List<Ingredient> ingredients) {
            this.ingredients = ingredients;
            return this;
        }

        public Builder brewTime(int brewTime) {
            this.brewTime = brewTime;
            return this;
        }

        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        public Builder brewDifficulty(int brewDifficulty) {
            this.brewDifficulty = brewDifficulty;
            return this;
        }

        public Builder cauldronType(CauldronType cauldronType) {
            this.cauldronType = cauldronType;
            return this;
        }

        public Builder barrelType(BarrelType barrelType) {
            this.barrelType = barrelType;
            return this;
        }

        public Builder agingYears(int agingYears) {
            this.agingYears = agingYears;
            return this;
        }

        public Builder distillRuns(int distillRuns) {
            this.distillRuns = distillRuns;
            return this;
        }

        public Builder distillTime(int distillTime) {
            this.distillTime = distillTime;
            return this;
        }

        public Builder alcohol(int alcohol) {
            this.alcohol = alcohol;
            return this;
        }

        public Builder names(Map<PotionQuality, String> names) {
            this.names = names;
            return this;
        }

        public Builder lore(Map<PotionQuality, List<String>> lore) {
            this.lore = lore;
            return this;
        }

        public Builder glint(boolean glint) {
            this.glint = glint;
            return this;
        }

        public Builder commands(Map<PotionQuality, List<String>> commands) {
            this.commands = commands;
            return this;
        }

        public Builder effects(List<RecipeEffect> effects) {
            this.effects = effects;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder actionBar(String actionBar) {
            this.actionBar = actionBar;
            return this;
        }

        public Recipe build() {
            if (ingredients.isEmpty()) {
                throw new IllegalStateException("Ingredients should not be empty");
            }
            return new Recipe(recipeName, brewTime, brewDifficulty, alcohol, cauldronType, ingredients, names, lore, color, glint, distillRuns, distillTime, barrelType, agingYears, commands, effects, title, message, actionBar);
        }
    }
}
