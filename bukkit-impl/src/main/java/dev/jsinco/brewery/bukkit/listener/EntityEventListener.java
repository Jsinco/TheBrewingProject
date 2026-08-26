package dev.jsinco.brewery.bukkit.listener;

import dev.jsinco.brewery.bukkit.effect.named.ChickenNamedExecutable;
import dev.jsinco.brewery.bukkit.recipe.RecipeEffectsImpl;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;

import java.util.Optional;

public class EntityEventListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(ChickenNamedExecutable.NO_DROPS)) {
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (!FeaturesConfig.test(FeatureFlag.BREW_DRINKING, event.getEntity().getWorld().getName())) {
            return;
        }
        Optional<RecipeEffectsImpl> recipeEffectsOptional = RecipeEffectsImpl.fromEntity(event.getEntity());
        recipeEffectsOptional.ifPresent(recipeEffects ->
                event.getAffectedEntities().stream()
                        .filter(Player.class::isInstance)
                        .map(Player.class::cast)
                        .forEach(recipeEffects::applyTo));
    }

}
