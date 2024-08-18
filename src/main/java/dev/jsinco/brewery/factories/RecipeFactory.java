package dev.jsinco.brewery.factories;

import dev.jsinco.brewery.TheBrewingProject;
import dev.jsinco.brewery.enums.BarrelType;
import dev.jsinco.brewery.enums.CauldronType;
import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.recipes.DefaultRecipe;
import dev.jsinco.brewery.recipes.Recipe;
import dev.jsinco.brewery.recipes.RecipeEffect;
import dev.jsinco.brewery.recipes.ReducedRecipe;
import dev.jsinco.brewery.recipes.ingredient.IngredientManager;
import dev.jsinco.brewery.util.FileUtil;
import dev.jsinco.brewery.util.Pair;
import dev.jsinco.brewery.util.Util;
import lombok.Getter;
import org.bukkit.potion.PotionEffectType;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeFactory {

    @Getter
    private final List<ReducedRecipe> reducedRecipes = new ArrayList<>();
    private final YamlFile recipesFile;

    public RecipeFactory() {
        Path mainDir = TheBrewingProject.getInstance().getDataFolder().toPath();
        FileUtil.extractFile(this.getClass(), "recipes.yml", mainDir, false);
        this.recipesFile = new YamlFile(mainDir.resolve("recipes.yml").toFile());

        try {
            this.recipesFile.createOrLoadWithComments();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ConfigurationSection recipes = recipesFile.getConfigurationSection("recipes");
        for (String recipeName : recipes.getKeys(false)) {
            this.reducedRecipes.add(this.getReducedRecipe(recipeName));
        }
    }


    /**
     * Obtain a ReducedRecipe from the recipes.yml file.
     * @param recipeName The name/id of the recipe to obtain. Ex: 'example_recipe'
     * @return A ReducedRecipe object with fewer attributes only used for identifying which recipe is being brewed
     */
    public ReducedRecipe getReducedRecipe(String recipeName) {
        ConfigurationSection recipe = recipesFile.getConfigurationSection("recipes." + recipeName);

        return new ReducedRecipe.Builder(recipeName)
                .brewTime(recipe.getInt("brew-time", 0))
                .brewDifficulty(recipe.getInt("brew-difficulty", 1))
                .cauldronType(Util.getEnumByName(CauldronType.class, recipe.getString("cauldron-type", "WATER")))
                .ingredients(IngredientManager.getIngredients(recipe.getStringList("ingredients")))
                .color(Util.parseColorString(recipe.getString("potion-attributes.color", "AQUA")))
                .distillRuns(recipe.getInt("distilling.runs", 0))
                .distillTime(recipe.getInt("distilling.time", 0))
                .barrelType(Util.getEnumByName(BarrelType.class, recipe.getString("aging.barrel-type", "ANY")))
                .agingYears(recipe.getInt("aging.years", 0))
                .build();
    }


    /**
     * Obtain a recipe from the recipes.yml file.
     * @param recipeName The name/id of the recipe to obtain. Ex: 'example_recipe'
     * @return A Recipe object with all the attributes of the recipe.
     */
    public Recipe getRecipe(String recipeName) {
        ConfigurationSection recipe = recipesFile.getConfigurationSection("recipes." + recipeName);

        return new Recipe.Builder(recipeName)
                .brewTime(recipe.getInt("brew-time", 0))
                .brewDifficulty(recipe.getInt("brew-difficulty", 1))
                .alcohol(this.parseAlcoholString(recipe.getString("alcohol", "0%")))
                .cauldronType(Util.getEnumByName(CauldronType.class, recipe.getString("cauldron-type")))
                .ingredients(IngredientManager.getIngredients(recipe.getStringList("ingredients")))
                .names(this.getQualityFactoredString(recipe.getString("potion-attributes.name")))
                .lore(this.getQualityFactoredList(recipe.getStringList("potion-attributes.lore")))
                .color(Util.parseColorString(recipe.getString("potion-attributes.color")))
                .glint(recipe.getBoolean("potion-attributes.glint", false))
                .distillRuns(recipe.getInt("distilling.runs", 0))
                .distillTime(recipe.getInt("distilling.time", 0))
                .barrelType(Util.getEnumByName(BarrelType.class, recipe.getString("aging.barrel-type", "ANY")))
                .agingYears(recipe.getInt("aging.years", 0))
                .commands(this.getQualityFactoredList(recipe.getStringList("commands")))
                .effects(this.getEffectsFromStringList(recipe.getStringList("effects")))
                .title(recipe.getString("messages.title", null))
                .message(recipe.getString("messages.message", null))
                .actionBar(recipe.getString("messages.action-bar", null))
                .build();
    }

    /**
     * Get a full Recipe from a ReducedRecipe
     * @param reducedRecipe The ReducedRecipe to get the full Recipe from
     * @return A Recipe object with all the attributes of the recipe.
     */
    public Recipe getRecipe(ReducedRecipe reducedRecipe) {
        ConfigurationSection recipe = recipesFile.getConfigurationSection("recipes." + reducedRecipe.getRecipeName());

        return new Recipe.Builder(reducedRecipe.getRecipeName())
                .brewTime(reducedRecipe.getBrewTime())
                .brewDifficulty(reducedRecipe.getBrewDifficulty())
                .alcohol(this.parseAlcoholString(recipe.getString("alcohol", "0%")))
                .cauldronType(reducedRecipe.getCauldronType())
                .ingredients(reducedRecipe.getIngredients())
                .names(this.getQualityFactoredString(recipe.getString("potion-attributes.name")))
                .lore(this.getQualityFactoredList(recipe.getStringList("potion-attributes.lore")))
                .color(reducedRecipe.getColor())
                .glint(recipe.getBoolean("potion-attributes.glint", false))
                .distillRuns(reducedRecipe.getDistillRuns())
                .distillTime(reducedRecipe.getDistillTime())
                .barrelType(reducedRecipe.getBarrelType())
                .agingYears(reducedRecipe.getAgingYears())
                .commands(this.getQualityFactoredList(recipe.getStringList("commands")))
                .effects(this.getEffectsFromStringList(recipe.getStringList("effects")))
                .title(recipe.getString("messages.title", null))
                .message(recipe.getString("messages.message", null))
                .actionBar(recipe.getString("messages.action-bar", null))
                .build();
    }


    /**
     * Obtain a default recipe from the recipes.yml file.
     * Default recipes are used when no recipe is found while brewing.
     * @param defaultRecipeName The name/id of the default recipe to obtain. Ex: 'cauldron_brew'
     * @return A DefaultRecipe object with all the attributes of the default recipe.
     */
    public DefaultRecipe getDefaultRecipe(String defaultRecipeName) {
        ConfigurationSection defaultRecipe = recipesFile.getConfigurationSection("default-recipes." + defaultRecipeName);

        return new DefaultRecipe.Builder()
                .name(defaultRecipe.getString("name", "Cauldron Brew"))
                .lore(defaultRecipe.getStringList("lore"))
                .color(Util.parseColorString(defaultRecipe.getString("color", "BLUE")))
                .customModelData(defaultRecipe.getInt("custom-model-data", -1))
                .glint(defaultRecipe.getBoolean("glint", false))
                .build();
    }



    // TODO: This should all be in a utility class

    private int parseAlcoholString(String str) {
        return Util.getInt(str.replace("%", "").replace(" ", ""));
    }

    // FIXME - I feel like there has to be a better way of doing this that doesn't rely on a map of enums?
    private Map<PotionQuality, String> getQualityFactoredString(String str) {
        if (!str.contains("/")) {
            return Map.of(PotionQuality.BAD, str, PotionQuality.GOOD, str, PotionQuality.EXCELLENT, str);
        }

        String[] list = str.split("/");
        Map<PotionQuality, String> map = new HashMap<>();

        for (int i = 0; i < Math.min(list.length, 3); i++) {
            map.put(PotionQuality.values()[i], list[i]);
        }

        return map;
    }

    private Map<PotionQuality, List<String>> getQualityFactoredList(List<String> list) {
        Map<PotionQuality, List<String>> map = new HashMap<>();

        for (String string : list) {
            if (string.startsWith("+")) {
                map.put(PotionQuality.BAD, list);
            } else if (string.startsWith("++")) {
                map.put(PotionQuality.GOOD, list);
            } else if (string.startsWith("+++")) {
                map.put(PotionQuality.EXCELLENT, list);
            } else {
                for (PotionQuality quality : PotionQuality.values()) {
                    map.put(quality, list);
                }
            }
        }

        return map;
    }

    private Pair<Integer, Integer> getNumberPair(String string) {
        if (!string.contains("-")) {
            int i = Util.getInt(string);
            return Pair.singleValue(i);
        }
        String[] split = string.split("-");
        return new Pair<>(Util.getInt(split[0]), Util.getInt(split[1]));
    }


    private List<RecipeEffect> getEffectsFromStringList(List<String> list) {
        List<RecipeEffect> effects = new ArrayList<>();
        for (String string : list) {
            effects.add(this.getEffect(string));
        }
        return effects;
    }


    private RecipeEffect getEffect(String string) {
        if (!string.contains("/")) {
            return RecipeEffect.of(PotionEffectType.getByName(string), Pair.singleValue(1), Pair.singleValue(1));
        }

        String[] parts = string.split("/");
        PotionEffectType effectType = PotionEffectType.getByName(parts[0]);
        Pair<Integer, Integer> durationBounds = getNumberPair(parts[1]);
        Pair<Integer, Integer> amplifierBounds;
        if (parts.length >= 3) {
            amplifierBounds = getNumberPair(parts[2]);
        } else {
            amplifierBounds = Pair.singleValue(1);
        }

        return RecipeEffect.of(effectType, durationBounds, amplifierBounds);
    }


}
