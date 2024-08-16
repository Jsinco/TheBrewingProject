package dev.jsinco.brewery.factories;

import dev.jsinco.brewery.TheBrewingProject;
import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.recipes.Recipe;
import dev.jsinco.brewery.recipes.ingredients.Ingredient;
import dev.jsinco.brewery.util.FileUtil;
import dev.jsinco.brewery.util.Util;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// this class isn't close to being finished, just ignore
public class RecipesLoader {

    YamlFile recipesFile;
    ConfigurationSection recipesSection;

    public RecipesLoader() {
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

    // Recipe constructor for reference:
    //(String recipeName, int brewTime, int brewDifficulty, int alcohol, CauldronType cauldronType,
    // List<Ingredient> ingredients, Map<PotionQuality, String> names,
    // Map<PotionQuality, List<String>> lore, Color color, boolean glint, int distillRuns,
    // int distillTime, BarrelType barrelType, int agingYears, Map<PotionQuality, List<String>> commands, Map<PotionQuality,
    // List<RecipeEffect>> effects, String title, String message, String actionBar)
    public Recipe getRecipe(String recipeName) {
        new Recipe(
            recipeName,
                recipesSection.getInt(recipeName + ".brew-time"),
                recipesSection.getInt(recipeName + ".brew-difficulty"),
                parseAlcoholString(recipesSection.getString(recipeName + ".alcohol")),
                recipesSection.getString(recipeName + ".cauldron-type"),
        )
    }

    private List<Ingredient> getIngredients(String key) {
        List<String> list = recipesSection.getStringList(key);
        List<Ingredient> ingredients = new ArrayList<>();

        for (String str :)
    }


    private int parseAlcoholString(String str) {
        return Util.getInt(str.replace("%", "").replace(" ", ""));
    }

    // FIXME - There has to be a better way of doing this that doesn't rely on a map
    // of enums.

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
}
