package dev.jsinco.brewery.api.breweries;

import dev.jsinco.brewery.api.structure.MultiblockStructure;
import dev.jsinco.brewery.api.util.CancelState;
import dev.jsinco.brewery.api.util.Holder;
import dev.jsinco.brewery.api.vector.Location;
import org.jspecify.annotations.NonNull;

public interface DistilleryAccess extends SelfSchedulingBrewery {
    /**
     * Open this distillery inventory for the player with the specified UUID
     *
     * @param location The location to open from
     * @param player   The player UUID
     * @return True if canceled
     */
    CancelState open(@NonNull Location location, Holder.@NonNull Player player);

    /**
     * Closes the distillery inventory for all viewers
     *
     * @param silent Whether to play a close sound or not
     */
    void close(boolean silent);

    /**
     * Destroy the distillery, dropping all contained items
     *
     * @param breweryLocation The location to destroy from
     */
    void destroy(Location breweryLocation);

    /**
     * @return This distillery mixture inventory
     */
    BrewInventory getMixture();

    /**
     * @return This distillery distillate inventory
     */
    BrewInventory getDistillate();

    /**
     * @return The underlying distillery structure
     */
    MultiblockStructure<? extends DistilleryAccess> getStructure();

}
