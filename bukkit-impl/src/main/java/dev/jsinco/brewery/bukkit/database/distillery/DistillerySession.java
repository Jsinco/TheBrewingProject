package dev.jsinco.brewery.bukkit.database.distillery;

import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.api.vector.Location;
import dev.jsinco.brewery.bukkit.breweries.distillery.BukkitDistillery;
import dev.jsinco.brewery.database.Session;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DistillerySession extends Session<DistillerySession> {

    CompletableFuture<Void> insertBrew(Location distilleryLocation, int inventoryPos, boolean distillateInventoryType, Brew brew);

    CompletableFuture<Void> removeBrew(Location distilleryLocation, int inventoryPos, boolean distillateInventoryType);

    CompletableFuture<List<BrewLookupResult>> findBrews(Location distilleryLocation);

    CompletableFuture<Void> updateBrew(Location distilleryLocation, int inventoryPos, boolean distillateInventoryType, Brew newBrew);

    CompletableFuture<Void> insertDistillery(BukkitDistillery distillery);

    CompletableFuture<Void> removeDistillery(BukkitDistillery distillery);

    CompletableFuture<List<BukkitDistillery>> findDistilleries(UUID worldUuid);

    CompletableFuture<Void> updateDistillery(BukkitDistillery newDistillery);

    record BrewLookupResult(Brew brew, int position, boolean distillateInventoryType) {
    }
}
