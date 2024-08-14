package dev.jsinco.brewery.recipes.ingredients;

import dev.jsinco.brewery.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SimpleIngredient implements Ingredient {

    private final Material material;
    private final int amount;

    public SimpleIngredient(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public static SimpleIngredient of(String simpleItemString) {
        String[] split = simpleItemString.split("/");
        return new SimpleIngredient(Material.getMaterial(split[0]), Util.getInt(split[1]));
    }

    public static SimpleIngredient get(ItemStack item) {
        return new SimpleIngredient(item.getType(), item.getAmount());
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        return itemStack.getType() == material && itemStack.getAmount() >= amount;
    }
}
