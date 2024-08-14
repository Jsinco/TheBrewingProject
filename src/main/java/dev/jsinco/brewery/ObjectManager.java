package dev.jsinco.brewery;

import dev.jsinco.brewery.objects.Cauldron;
import dev.jsinco.brewery.recipes.ReducedRecipe;
import dev.jsinco.brewery.recipes.ingredients.Ingredient;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which stores lists of all Brewery objects
 */
@Getter
public final class ObjectManager {

    @Getter
    private static final List<Ingredient> acceptableIngredients = new ArrayList<>();

    @Getter
    private static final List<ReducedRecipe> reducedRecipes = new ArrayList<>();

    @Getter
    private static final List<Cauldron> activeCauldrons = new ArrayList<>();

}
