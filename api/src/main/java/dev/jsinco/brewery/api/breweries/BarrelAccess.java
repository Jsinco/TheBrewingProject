package dev.jsinco.brewery.api.breweries;

import dev.jsinco.brewery.api.structure.MultiblockStructure;
import dev.jsinco.brewery.api.util.CancelState;
import dev.jsinco.brewery.api.util.Holder;
import dev.jsinco.brewery.api.vector.Location;
import org.jspecify.annotations.NonNull;

public interface BarrelAccess extends SelfSchedulingBrewery {

    /**
     * Open this barrels inventory for the player with the specified UUID
     *
     * @param location The location to open from
     * @param player   The player
     * @return The resulting state
     */
    CancelState open(@NonNull Location location, Holder.@NonNull Player player);

    /**
     * Closes the barrel inventory for all viewers
     *
     * @param silent Whether to play a barrel sound or not
     */
    void close(boolean silent);

    /**
     * Destroy the barrel, dropping all contained items
     *
     * @param breweryLocation The location to destroy from
     */
    void destroy(Location breweryLocation);

    /**
     * @return The barrels inventory
     */
    BrewInventory getBrewInventory();

    /**
     * @return The underlying barrel structure
     */
    MultiblockStructure<? extends BarrelAccess> getStructure();
}
