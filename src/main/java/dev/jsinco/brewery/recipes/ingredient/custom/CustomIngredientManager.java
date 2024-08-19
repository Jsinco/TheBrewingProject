package dev.jsinco.brewery.recipes.ingredient.custom;

import dev.jsinco.brewery.TheBrewingProject;
import dev.jsinco.brewery.recipes.ingredient.PluginIngredient;
import dev.jsinco.brewery.util.FileUtil;
import dev.jsinco.brewery.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CustomIngredientManager extends PluginIngredient {

    // FIXME: I don't feel good about this implementation. It feels slow. Everytime we're getting an ingredient ItemStack we're looping through every CustomItem - Jsinco
    private static final List<CustomIngredient> customIngredients = new ArrayList<>();


    public static void reloadCustomIngredients() {
        Path mainDir = TheBrewingProject.getInstance().getDataFolder().toPath();
        FileUtil.extractFile(TheBrewingProject.class, "custom-ingredients.yml", mainDir, false);
        YamlFile customIngredientsFile = new YamlFile(mainDir.resolve("custom-ingredients.yml").toFile());

        try {
            customIngredientsFile.createOrLoadWithComments();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!customIngredients.isEmpty()) {
            customIngredients.clear();
        }

        ConfigurationSection customIngredientsSection = customIngredientsFile.getConfigurationSection("custom-ingredients");
        for (String id : customIngredientsSection.getKeys(false)) {
            ConfigurationSection ingredientSection = customIngredientsSection.getConfigurationSection(id);


            CustomIngredient customIngredient = new CustomIngredient(id,
                    ingredientSection.getString("name", null),
                    ingredientSection.getStringList("lore"),
                    Util.getEnumByName(Material.class, ingredientSection.getString("material", null)),
                    ingredientSection.getInt("custom-model-data", -1));
            customIngredients.add(customIngredient);
        }
    }

    @Override
    public String getItemIdByItemStack(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return null;
        }

        for (CustomIngredient customIngredient : customIngredients) {
            if (itemMeta.hasDisplayName() && !itemMeta.getDisplayName().equals(customIngredient.name())) {
                continue;
            } else if (itemMeta.hasLore() && !itemMeta.getLore().equals(customIngredient.lore())) {
                continue;
            } else if (itemMeta.hasCustomModelData() && itemMeta.getCustomModelData() != customIngredient.customModelData()) {
                continue;
            } else if (itemMeta.getCustomModelData() != customIngredient.customModelData()) {
                continue;
            }
            return customIngredient.id();
        }
        return null;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        return this.getItemIdByItemStack(itemStack) != null;
    }
}
