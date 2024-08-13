package dev.jsinco.brewery.recipes;

import lombok.Getter;

import java.util.List;

/**
 * Recipe object with fewer attributes only used for identifying which recipe is being brewed
 */
@Getter
public record ReducedRecipe(List<Ingredient> ingredients, String color, int cookingTime) {

}
