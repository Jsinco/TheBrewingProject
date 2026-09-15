package dev.jsinco.brewery.bukkit.breweries.barrel;

import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.vector.Location;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.breweries.BrewInventoryImpl;
import dev.jsinco.brewery.bukkit.breweries.BrewPersistenceHandler;
import dev.jsinco.brewery.bukkit.database.SessionTypes;
import dev.jsinco.brewery.bukkit.database.barrel.BarrelSession;
import dev.jsinco.brewery.database.PersistenceException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class BarrelBrewPersistenceHandler implements BrewPersistenceHandler {

    private final Location unique;

    public BarrelBrewPersistenceHandler(Location unique) {
        this.unique = unique;
    }

    @Override
    public void store(@Nullable Brew brew, int position, @NonNull BrewInventoryImpl inventory) {
        if (Objects.equals(inventory.getBrews()[position], brew)) {
            return;
        }
        try {
            BarrelSession session = TheBrewingProject.getInstance().getDatabase().startSession(SessionTypes.BARREL_SESSION_TYPE);
            Brew previous = inventory.getBrews()[position];
            if (previous == null) {
                session.insertBrew(unique, position, brew)
                        .exceptionally(Logger::logAndTrackErr);
                return;
            }
            if (brew == null) {
                session.removeBrew(unique, position)
                        .exceptionally(Logger::logAndTrackErr);
                return;
            }
            session.updateBrew(unique, position, brew)
                    .exceptionally(Logger::logAndTrackErr);
        } catch (PersistenceException e) {
            Logger.logAndTrackErr(e);
        }
    }
}
