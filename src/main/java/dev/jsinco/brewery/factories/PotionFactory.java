package dev.jsinco.brewery.factories;

import dev.jsinco.brewery.TheBrewingProject;
import dev.jsinco.brewery.enums.PotionQuality;
import dev.jsinco.brewery.objects.Cauldron;
import dev.jsinco.brewery.recipes.Recipe;
import dev.jsinco.brewery.recipes.RecipeEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

public class PotionFactory {

    private static final NamespacedKey RECIPE_NAME_KEY = new NamespacedKey(TheBrewingProject.getInstance(), "recipe");

    private final Cauldron cauldron;
    private final Recipe recipe = null;

    public PotionFactory(Cauldron cauldron) {
        this.cauldron = cauldron;
        // Implement some method to grab a recipe from a ReducedRecipe
        throw new UnsupportedOperationException("Not implemented");
    }


    // FIXME - This method is incomplete
    @SuppressWarnings("ConstantConditions") // Suppress 'meta' might be null warning
    public ItemStack create() {
        // Fixme - needs to factor in for distilling and aging

        PotionQuality quality = cauldron.getPotionQuality();

        if (recipe == null || quality == null) {
            return getRandomDefaultPotion();
        }

        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();


        // FIXME - Remove 'ItemFlag.HIDE_POTION_EFFECTS' due to API changes between 1.20.4 - 1.20.6?
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(RECIPE_NAME_KEY, PersistentDataType.STRING, recipe.getRecipeName());

        meta.setDisplayName(recipe.getNameBasedOnQuality(quality));
        meta.setLore(recipe.getLoreBasedOnQuality(quality));
        meta.setColor(recipe.getColor());


        for (RecipeEffect recipeEffect : recipe.getEffects()) {
            meta.addCustomEffect(recipeEffect.getPotionEffect(quality), true);
        }

        if (recipe.isGlint()) {
            meta.addEnchant(Enchantment.MENDING, 1, true);
        }

        item.setItemMeta(meta);
        return item;
    }


    public static ItemStack getRandomDefaultPotion() {
        // impl to get a default potion
        throw new UnsupportedOperationException("Not implemented");
    }
}
