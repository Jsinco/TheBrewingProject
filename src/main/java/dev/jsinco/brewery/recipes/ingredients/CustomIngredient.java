package dev.jsinco.brewery.recipes.ingredients;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class CustomIngredient implements Ingredient {

    @Getter
    private static final List<CustomIngredient> customIngredients = new ArrayList<>();

    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int customModelData;


    public CustomIngredient(Material material, String name, List<String> lore, int customModelData) {
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;

        customIngredients.add(this);
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return material == itemStack.getType() && name.equals(meta.getDisplayName()) && lore.equals(meta.getLore()) && customModelData == meta.getCustomModelData();
    }

    @Nullable
    public static CustomIngredient get(ItemStack item) {
        if (!item.hasItemMeta()) return null;

        for (CustomIngredient customIngredient : customIngredients) {
            if (customIngredient.matches(item)) {
                return customIngredient;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomIngredient that = (CustomIngredient) o;
        return customModelData == that.customModelData && material == that.material && Objects.equals(name, that.name) && Objects.equals(lore, that.lore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, name, lore, customModelData);
    }
}
