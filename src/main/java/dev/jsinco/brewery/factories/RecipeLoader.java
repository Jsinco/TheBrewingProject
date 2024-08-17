package dev.jsinco.brewery.factories;

import dev.jsinco.brewery.TheBrewingProject;
import dev.jsinco.brewery.enums.BarrelType;
import dev.jsinco.brewery.enums.CauldronType;
import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.recipes.Recipe;
import dev.jsinco.brewery.recipes.RecipeEffect;
import dev.jsinco.brewery.recipes.ingredient.IngredientManager;
import dev.jsinco.brewery.util.FileUtil;
import dev.jsinco.brewery.util.Pair;
import dev.jsinco.brewery.util.Util;
import org.bukkit.potion.PotionEffectType;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// this class isn't close to being finished, just ignore
public class RecipeLoader {

    private final YamlFile recipesFile;
    private final ConfigurationSection recipesSection;

    public RecipeLoader() {
        Path mainDir = TheBrewingProject.getInstance().getDataFolder().toPath();
        FileUtil.extractFile(this.getClass(), "recipes.yml", mainDir, false);
        this.recipesFile = new YamlFile(mainDir.resolve("recipes.yml").toFile());
        try {
            this.recipesFile.createOrLoadWithComments();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.recipesSection = this.recipesFile.getConfigurationSection("recipes");
    }

    /**
     * Obtain all recipes declared under the 'recipes' category in the recipes.yml file.
     * This method should be run asynchronously because there is an innumerable amount of recipes that could be loaded.
     * @return A list of Recipe objects with all the attributes of the recipes.
     */
    public List<Recipe> getRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        for (String recipeName : recipesSection.getKeys(false)) {
            recipes.add(this.getRecipe(recipeName));
        }
        return recipes;
    }

    /**
     * Obtain a recipe from the recipes.yml file.
     * @param recipeName The name/id of the recipe to obtain. Ex: 'example_recipe'
     * @return A Recipe object with all the attributes of the recipe.
     */
    public Recipe getRecipe(String recipeName) {
        ConfigurationSection recipe = recipesSection.getConfigurationSection(recipeName);

        return Recipe.builder(recipeName)
                .brewTime(recipe.getInt("brew-time", 0))
                .brewDifficulty(recipe.getInt("brew-difficulty", 1))
                .alcohol(this.parseAlcoholString(recipe.getString("alcohol", "0%")))
                .cauldronType(Util.getEnumByName(CauldronType.class, recipe.getString("cauldron-type")))
                .ingredients(IngredientManager.getIngredients(recipe.getStringList("ingredients")))
                .names(this.getQualityFactoredString(recipe.getString("potion-attributes.name")))
                .lore(this.getQualityFactoredList(recipe.getString("potion-attributes.lore")))
                .color(Util.parseColorString(recipe.getString("potion-attributes.color")))
                .glint(recipe.getBoolean("potion-attributes.glint", false))
                .distillRuns(recipe.getInt("distilling.runs", 0))
                .distillTime(recipe.getInt("distilling.time", 0))
                .barrelType(Util.getEnumByName(BarrelType.class, recipe.getString("aging.barrel-type", "ANY")))
                .agingYears(recipe.getInt("aging.years", 0))
                .commands(this.getQualityFactoredList(recipe.getString("commands")))
                .effects(this.getEffectsFromStringList(recipe.getStringList("effects")))
                .title(recipe.getString("messages.title", null))
                .message(recipe.getString("messages.message", null))
                .actionBar(recipe.getString("messages.action-bar", null))
                .build();
    }

    // TODO: This should all be in a utility class

    private int parseAlcoholString(String str) {
        return Util.getInt(str.replace("%", "").replace(" ", ""));
    }

    // FIXME - There has to be a better way of doing this that doesn't rely on a map of enums.
    private Map<PotionQuality, String> getQualityFactoredString(String key) {
        String str = recipesSection.getString(key);
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

    private Map<PotionQuality, List<String>> getQualityFactoredList(String key) {
        List<String> list = recipesSection.getStringList(key);
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
            return new Pair<>(i, i);
        }
        String[] split = string.split("-");
        return new Pair<>(Util.getInt(split[0]), Util.getInt(split[1]));
    }

    // This needs to be rewritten properly and cleanly to factor in for all possible scenarios of 'effects'
    // POISION/1 EFFECT + STATIC AMPLIFIER
    // POSION/1-4 EFFECT + DYDNAMIC AMPLIFIER
    // POISON/1/30 EFFECT + STATIC AMPLIFIER + STATIC TIME
    // POISON/1-4/30-60 EFFECT + DYNAMIC AMPLIFIER + DYNAMIC TIME
    private List<RecipeEffect> getEffectsFromStringList(List<String> list) {
        List<RecipeEffect> effects = new ArrayList<>();
        for (String string : list) {
            String[] parts = string.split("/");
            PotionEffectType effectType = PotionEffectType.getByName(parts[0]);
            Pair<Integer, Integer> amplifierBounds = getNumberPair(parts[1]);
            Pair<Integer, Integer> durationBounds = getNumberPair(parts[2]);
        }
        return effects;
    }


}
