package dev.jsinco.brewery.bukkit.listener;

import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.bukkit.breweries.BreweryRegistry;
import dev.jsinco.brewery.bukkit.breweries.barrel.BukkitBarrel;
import dev.jsinco.brewery.bukkit.breweries.distillery.BukkitDistillery;
import dev.jsinco.brewery.bukkit.database.SessionTypes;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import dev.jsinco.brewery.database.PersistenceException;
import dev.jsinco.brewery.database.sql.SqlDatabase;
import dev.jsinco.brewery.structure.PlacedStructureRegistryImpl;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldEventListener implements Listener {

    private final SqlDatabase database;
    private final PlacedStructureRegistryImpl placedStructureRegistry;
    private final BreweryRegistry registry;

    public WorldEventListener(SqlDatabase database, PlacedStructureRegistryImpl placedStructureRegistry, BreweryRegistry registry) {
        this.database = database;
        this.placedStructureRegistry = placedStructureRegistry;
        this.registry = registry;
    }

    public void init() {
        Bukkit.getServer().getWorlds().forEach(this::loadWorld);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent event) {
        loadWorld(event.getWorld());
    }

    public void onWorldUnload(WorldUnloadEvent event) {
        placedStructureRegistry.unloadWorld(event.getWorld().getUID());
    }

    private void loadWorld(World world) {
        try {
            if (FeaturesConfig.test(FeatureFlag.BARRELS, world.getName())) {
                database.startSession(SessionTypes.BARREL_SESSION_TYPE).findBarrels(world.getUID())
                        .thenAccept(barrels -> {
                            placedStructureRegistry.registerStructures(barrels.stream().map(BukkitBarrel::getStructure).toList());
                            registry.registerInventories(barrels);
                        }).exceptionally(Logger::logAndTrackErr);
            }
            if (FeaturesConfig.test(FeatureFlag.CAULDRONS, world.getName())) {
                database.startSession(SessionTypes.CAULDRON_SESSION_TYPE).findCauldrons(world.getUID())
                        .thenAccept(cauldrons -> {
                            cauldrons.forEach(registry::addActiveSinglePositionStructure);
                        }).exceptionally(Logger::logAndTrackErr);
            }
            if (FeaturesConfig.test(FeatureFlag.DISTILLERIES, world.getName())) {
                database.startSession(SessionTypes.DISTILLERY_SESSION_TYPE).findDistilleries(world.getUID())
                        .thenAccept(distilleries -> {
                            placedStructureRegistry.registerStructures(distilleries.stream().map(BukkitDistillery::getStructure).toList());
                            registry.registerInventories(distilleries);
                        }).exceptionally(Logger::logAndTrackErr);
            }
        } catch (PersistenceException e) {
            Logger.logErr(e);
        }
    }
}
