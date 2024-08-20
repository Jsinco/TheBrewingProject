package dev.jsinco.brewery.recipes.ingredient.custom;

import org.bukkit.Material;

import java.util.List;

/**
 * Encapsulates a custom ingredient defined in 'custom-ingredients.yml'.
 * @param id The identifier of the custom ingredient.
 * @param name The name of the custom ingredient.
 * @param lore The lore of the custom ingredient.
 * @param material The material of the custom ingredient.
 * @param customModelData The custom model data of the custom ingredient.
 */
public record CustomIngredient(String id, String name, List<String> lore, Material material, int customModelData) {}
