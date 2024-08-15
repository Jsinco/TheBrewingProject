package dev.jsinco.brewery.objects;

import dev.jsinco.brewery.util.BlockUtil;
import dev.jsinco.brewery.util.CoreConfiguration;
import dev.jsinco.brewery.ObjectManager;
import dev.jsinco.brewery.recipes.ReducedRecipe;
import dev.jsinco.brewery.recipes.ingredients.Ingredient;
import dev.jsinco.brewery.util.Util;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Getter
public class Cauldron implements Tickable {

    private static final Random RANDOM = new Random();

    private final UUID uid;
    private final List<Ingredient> ingredients;
    private final Block block;


    private int brewTime = 0;

    // To determine the closest recipe:
    // Every time @tick is run, check all reducedrecipes/recipes list and see if the ingredients match
    // if they do, set the closest recipe to that recipe
    private ReducedRecipe closestRecipe = null;
    // To determine particle effect color:
    // Every time @tick is run and if closest recipe is NOT null, get color from closest recipe
    // and gradually shift color to it. If closest recipe becomes null, reset this back to AQUA
    private Color particleColor = Color.AQUA;


    public Cauldron(Block block) {
        this.uid = UUID.randomUUID();
        this.ingredients = new ArrayList<>();
        this.block = block;
    }

    // Generally for loading from persistent storage
    public Cauldron(UUID uid, List<Ingredient> ingredients, Block block, int brewTime, ReducedRecipe closestRecipe, Color particleColor) {
        this.uid = uid;
        this.ingredients = ingredients;
        this.block = block;
        this.brewTime = brewTime;
        this.closestRecipe = closestRecipe;
        this.particleColor = particleColor;
    }


    @Override
    public void tick() {
        if (!this.isOnHeatSource() || (this.closestRecipe != null && !this.cauldronTypeMatchesRecipe())) {
            this.remove();
            return;
        }
        this.determineClosestRecipe();
        this.updateParticleColor();
        this.brewTime++;
    }



    public void create() {
        ObjectManager.getActiveCauldrons().add(this);
    }

    public void remove() {
        ObjectManager.getActiveCauldrons().remove(this);
    }


    public void addIngredient(ItemStack item, Player player) {
        this.addIngredient(Ingredient.getIngredient(item), player);
    }

    public void addIngredient(Ingredient ingredient, Player player) {
        // Todo: Add API event
        // Todo: Add permission check
        ingredients.add(ingredient);
    }


    public void determineClosestRecipe() {
        if (this.closestRecipe != null && this.closestRecipe.getIngredients().equals(this.ingredients)) {
            return; // Don't check if already determined and ingredients haven't changed
        }

        for (ReducedRecipe reducedRecipe : ObjectManager.getReducedRecipes()) {
            // Don't even bother checking recipes that don't have the same amount of ingredients
            if (this.ingredients.size() != reducedRecipe.getIngredients().size()) continue;

            boolean match = true;

            for (Ingredient ingredient : this.ingredients) {
                if (!reducedRecipe.getIngredients().contains(ingredient)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                this.closestRecipe = reducedRecipe;
                return; // Found a match
            }
        }
        this.closestRecipe = null; // Couldn't find a match
    }

    public void updateParticleColor() {
        if (this.closestRecipe == null && this.particleColor != Color.AQUA) {
            this.particleColor = Color.AQUA;
        } else if (this.closestRecipe != null) {
            this.particleColor = Util.getNextColor(particleColor, this.closestRecipe.getColor(), brewTime, this.closestRecipe.getBrewTime());
        }
    }


    public boolean isOnHeatSource() {
        if (CoreConfiguration.cauldronHeatSources.isEmpty()) {
            return true;
        }

        Block blockBelow = block.getRelative(0, -1, 0);
        Material below = blockBelow.getType();
        if (below == Material.CAMPFIRE || below == Material.SOUL_CAMPFIRE) {
            return BlockUtil.isLitCampfire(blockBelow);
        } else if (below == Material.LAVA || below == Material.WATER) {
            return BlockUtil.isSource(blockBelow);
        }
        return CoreConfiguration.cauldronHeatSources.contains(below);
    }


    public boolean cauldronTypeMatchesRecipe() {
        return closestRecipe.getCauldronType().getMaterial() == block.getType();
    }


    public void playBrewingEffects() {
        Location particleLoc = // Complex particle location based off BreweryX
                block.getLocation().add(0.5 + (RANDOM.nextDouble() * 0.8 - 0.4), 0.9, 0.5 + (RANDOM.nextDouble() * 0.8 - 0.4));

        block.getWorld().spawnParticle(Particle.SPELL_MOB, particleLoc, 0, particleColor);


        if (!CoreConfiguration.cauldronMinimalParticles) {
            return;
        }

        if (RANDOM.nextFloat() > 0.85) {
            // Dark pixely smoke cloud at 0.4 random in x and z
            // 0 count enables direction, send to y = 1 with speed 0.09
            block.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 0, 0, 1, 0, 0.09);
        }
        if (RANDOM.nextFloat() > 0.2) {
            // A Water Splash with 0.2 offset in x and z
            block.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLoc, 1, 0.2, 0, 0.2);
        }
        if (RANDOM.nextFloat() > 0.4) {
            // Two hovering pixely dust clouds, a bit of offset and with DustOptions to give some color and size
            block.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 2, 0.15, 0.2, 0.15, new Particle.DustOptions(particleColor, 1.5f));
        }
    }


    public ItemStack createPotion() {
        // Todo - What needs to happen here:
        // this should be called after a player clicks the cauldron with a glass bottle
        // this should check if the closest recipe is not null
        // if it's not null, we get the Recipe from our ReducedRecipe and create the potion
        // Then, lower the cauldron by 1 level. If it's empty, remove it
        // Finally, return the potion
        throw new UnsupportedOperationException("Not implemented yet");
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Cauldron cauldron = (Cauldron) obj;
        return uid.equals(cauldron.uid);
    }
}
