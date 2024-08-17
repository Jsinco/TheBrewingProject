package dev.jsinco.brewery.recipes.ingredient;

import dev.jsinco.brewery.util.Util;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Represents a simple ingredient that only consists of a material and an amount
 */
@AllArgsConstructor
public class SimpleIngredient extends Ingredient {

    private final Material material;

    public SimpleIngredient(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        return itemStack.getType() == material;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleIngredient that = (SimpleIngredient) o;
        return material == that.material && amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(material);
    }

    public static SimpleIngredient of(ItemStack itemStack) {
        return new SimpleIngredient(itemStack.getType());
    }

    public static SimpleIngredient of(String materialStr, int amount) {
        return new SimpleIngredient(Util.getEnumByName(Material.class, materialStr), amount);
    }
}
