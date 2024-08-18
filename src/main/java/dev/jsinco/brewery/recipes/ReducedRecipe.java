package dev.jsinco.brewery.recipes;

import dev.jsinco.brewery.enums.BarrelType;
import dev.jsinco.brewery.enums.CauldronType;
import dev.jsinco.brewery.recipes.ingredient.Ingredient;
import lombok.Getter;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe object with fewer attributes only used for identifying which recipe is being brewed
 */
@Getter
public class ReducedRecipe {

    // TODO: re-add specific heat sources

    protected final String recipeName;

    // Used for identifying and for particle effects while brewing in a cauldron
    protected final List<Ingredient> ingredients;
    protected final int brewTime;
    protected final Color color;
    protected final int brewDifficulty;
    protected final CauldronType cauldronType;

    // Used for aging
    protected final BarrelType barrelType;
    protected final int agingYears;

    // Used for distilling
    protected final int distillRuns;
    protected final int distillTime;


    public ReducedRecipe(String recipeName, List<Ingredient> ingredients, int brewTime, Color color, int brewDifficulty, CauldronType cauldronType, BarrelType barrelType, int agingYears, int distillRuns, int distillTime) {
        this.recipeName = recipeName;
        this.ingredients = ingredients;
        this.brewTime = brewTime;
        this.color = color == null ? Color.BLUE : color;
        this.brewDifficulty = brewDifficulty;
        this.cauldronType = cauldronType == null ? CauldronType.WATER : cauldronType;
        this.barrelType = barrelType == null ? BarrelType.ANY : barrelType;
        this.agingYears = agingYears;
        this.distillRuns = distillRuns;
        this.distillTime = distillTime;
    }


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
        private int distillTime = 30;

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

        public ReducedRecipe build() {
            if (ingredients.isEmpty()) {
                throw new IllegalStateException("Ingredients should not be empty");
            }
            return new ReducedRecipe(recipeName, ingredients, brewTime, color, brewDifficulty, cauldronType, barrelType, agingYears, distillRuns, distillTime);
        }
    }
}
