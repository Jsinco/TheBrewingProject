package dev.jsinco.brewery.objects;

import dev.jsinco.brewery.recipes.ReducedRecipe;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which stores lists of all necessary objects
 * AKA. (Reduced)Recipes, Cauldrons, Barrels, BreweryPlayers
 */
// TODO: change to instantiated rather than static class
public final class ObjectManager {

    @Getter
    private static final List<ReducedRecipe> reducedRecipes = new ArrayList<>();
    @Getter
    private static final List<Cauldron> activeCauldrons = new ArrayList<>();
}
