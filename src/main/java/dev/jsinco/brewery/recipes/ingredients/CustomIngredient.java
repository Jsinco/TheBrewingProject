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

    private final String id;
    private final List<Material> materialMatches;
    private final List<String> nameMatches;
    private final List<String> loreMatches;
    private final List<Integer> customModelDataMatches;

    public CustomIngredient(String id, List<Material> materialMatches, List<String> nameMatches, List<String> loreMatches, List<Integer> customModelDataMatches) {
        this.id = id;
        this.materialMatches = materialMatches;
        this.nameMatches = nameMatches;
        this.loreMatches = loreMatches;
        this.customModelDataMatches = customModelDataMatches;
    }

    @Override // TODO
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

    @Override // TODO
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
