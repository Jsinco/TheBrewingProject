package dev.jsinco.brewery.bukkit.breweries.distillery;

import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.api.vector.Location;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.breweries.BrewInventoryImpl;
import dev.jsinco.brewery.bukkit.breweries.BrewPersistenceHandler;
import dev.jsinco.brewery.bukkit.database.SessionTypes;
import dev.jsinco.brewery.bukkit.database.distillery.DistillerySession;
import dev.jsinco.brewery.database.PersistenceException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class DistilleryBrewPersistenceHandler implements BrewPersistenceHandler {


    private final Location unique;
    private final boolean distillate;

    public DistilleryBrewPersistenceHandler(Location unique, boolean distillate) {
        this.unique = unique;
        this.distillate = distillate;
    }


    /**
     * Set an item in the inventory and store the changes in the database
     *
     * @param brew
     * @param position
     */
    @Override
    public void store(@Nullable Brew brew, int position, @NonNull BrewInventoryImpl inventory) {
        if (Objects.equals(inventory.getBrews()[position], brew)) {
            return;
        }
        try {
            DistillerySession session = TheBrewingProject.getInstance().getDatabase().startSession(SessionTypes.DISTILLERY_SESSION_TYPE);
            Brew previous = inventory.getBrews()[position];
            if (previous == null) {
                session.insertBrew(unique, position, distillate, brew)
                        .exceptionally(Logger::logAndTrackErr);
                return;
            }
            if (brew == null) {
                session.removeBrew(unique, position, distillate)
                        .exceptionally(Logger::logAndTrackErr);
                return;
            }
            session.updateBrew(unique, position, distillate, brew)
                    .exceptionally(Logger::logAndTrackErr);
        } catch (PersistenceException e) {
            Logger.logErr(e);
        }

    }
}
