package dev.jsinco.brewery.factories;

import dev.jsinco.brewery.TheBrewingProject;
import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.objects.Cauldron;
import dev.jsinco.brewery.recipes.DefaultRecipe;
import dev.jsinco.brewery.recipes.Recipe;
import dev.jsinco.brewery.recipes.RecipeEffect;
import dev.jsinco.brewery.recipes.ReducedRecipe;
import dev.jsinco.brewery.util.Util;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

public class PotionFactory {

    private static final NamespacedKey RECIPE_NAME_KEY = new NamespacedKey(TheBrewingProject.getInstance(), "recipe");

    private final RecipeFactory recipeFactory = TheBrewingProject.getRecipeFactory();
    private final PotionQuality quality;
    private final ReducedRecipe reducedRecipe;

    public PotionFactory(Cauldron cauldron) {
        this.quality = cauldron.getPotionQuality();
        this.reducedRecipe = cauldron.getClosestRecipe();
    }


    @SuppressWarnings("ConstantConditions") // Suppress 'meta' might be null warning
    public ItemStack create() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);


        if (reducedRecipe == null || quality == null) {
            this.setRandomDefaultPotionMeta(meta);

        } else if (reducedRecipe.getDistillRuns() > 0 || reducedRecipe.getAgingYears() > 0) {
            this.setIncompletePotionMeta(meta);
        } else {
            this.setCompletePotionMeta(meta);
        }

        item.setItemMeta(meta);
        return item;
    }


    /**
     * Set the meta for a complete potion. This means that the cauldron found a recipe and that recipe does not require
     * aging or distilling
     * @param meta The potion meta to modify
     */
    private void setCompletePotionMeta(PotionMeta meta) {
        Recipe recipe = recipeFactory.getRecipe(reducedRecipe.getRecipeName());

        meta.getPersistentDataContainer().set(RECIPE_NAME_KEY, PersistentDataType.STRING, recipe.getRecipeName());
        meta.setDisplayName(recipe.getNameBasedOnQuality(quality));
        meta.setLore(recipe.getLoreBasedOnQuality(quality));
        meta.setColor(recipe.getColor());
        if (recipe.isGlint()) meta.addEnchant(Enchantment.MENDING, 1, true);
        if (recipe.getCustomModelData() > 0) meta.setCustomModelData(recipe.getCustomModelData());

        for (RecipeEffect recipeEffect : recipe.getEffects()) {
            meta.addCustomEffect(recipeEffect.getPotionEffect(quality), true);
        }
    }


    /**
     * Set the meta for an incomplete potion. This means that the cauldron found a recipe, but it requires aging or distilling
     * @param meta The potion meta to modify
     */
    private void setIncompletePotionMeta(PotionMeta meta) {
        meta.setDisplayName("Unfinished Brew");
        meta.getPersistentDataContainer().set(RECIPE_NAME_KEY, PersistentDataType.STRING, reducedRecipe.getRecipeName());
        meta.setColor(Util.getRandomElement(Util.NAME_TO_COLOR_MAP.values().stream().toList()));
    }



    /**
     * Sets the meta for a random default potion. This means the cauldron did not find a recipe.
     * @param meta The potion meta to modify
     */
    private void setRandomDefaultPotionMeta(PotionMeta meta) {
        DefaultRecipe defaultRecipe = recipeFactory.getRandomDefaultRecipe();

        meta.setDisplayName(defaultRecipe.getName());
        meta.setLore(defaultRecipe.getLore());
        meta.setColor(defaultRecipe.getColor());
        if (defaultRecipe.isGlint()) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
        }
        if (defaultRecipe.getCustomModelData() > 0) {
            meta.setCustomModelData(defaultRecipe.getCustomModelData());
        }
    }
}
