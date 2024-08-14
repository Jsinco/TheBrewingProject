package dev.jsinco.brewery.objects;

import dev.jsinco.brewery.ObjectManager;
import dev.jsinco.brewery.recipes.ingredients.Ingredient;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Cauldron implements Tickable {

    private final UUID uid;
    private final List<Ingredient> ingredients;
    private final Block block;
    private final int brewTime;

    public Cauldron(Block block) {
        this.uid = UUID.randomUUID();
        this.ingredients = new ArrayList<>();
        this.block = block;
        this.brewTime = 0;
    }

    public Cauldron(UUID uid, List<Ingredient> ingredients, Block block, int brewTime) {
        this.uid = uid;
        this.ingredients = ingredients;
        this.block = block;
        this.brewTime = brewTime;
    }


    public void create() {
        ObjectManager.getActiveCauldrons().add(this);
    }

    public void remove() {
        ObjectManager.getActiveCauldrons().remove(this);
    }


    public void addIngredient(Ingredient ingredient) {
        ingredients.add(ingredient);
    }
    public void addIngredient(ItemStack item) {
        Ingredient ingredient = Ingredient.getIngredient(item);
        ingredients.add(ingredient);
    }






    @Override
    public void tick() {

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Cauldron cauldron = (Cauldron) obj;
        return uid.equals(cauldron.uid);
    }
}
