package dev.jsinco.brewery.api.structure;

import dev.jsinco.brewery.api.breweries.StructureHolder;
import dev.jsinco.brewery.api.vector.Location;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * ALL OPERATIONS DONE ON THIS REGISTRY ARE <b>NOT</b> GOING TO BE PERSISTENT
 */
public interface PlacedStructureRegistry {

    /**
     * @param placedBreweryStructure The multi block structure to register
     */
    void registerStructure(MultiblockStructure<?> placedBreweryStructure);

    /**
     * @param structure The multiblock structure to unregister
     */
    void unregisterStructure(MultiblockStructure<?> structure);

    /**
     * @param location The location to check for a structure
     * @return An optionally present structure if matches
     */
    Optional<MultiblockStructure<?>> getStructure(Location location);

    /**
     * @param locations The locations to check for structures
     * @return A set of all matching structures
     */
    Set<MultiblockStructure<?>> getStructures(Collection<Location> locations);

    /**
     *
     * @param structureType The structure type to count
     * @return The amount of structures of that type
     */
    int countStructureType(StructureType<?> structureType);

    /**
     * Utility method
     *
     * @param location The location to check for a structure
     * @return An optionally present structure holder if matches
     */
    default Optional<StructureHolder<?>> getHolder(Location location) {
        return getStructure(location).map(MultiblockStructure::getHolder);
    }

    /**
     * @param worldUuid The world to unload all structures in
     */
    void unloadWorld(UUID worldUuid);

    /**
     * Clear this registry
     */
    void clear();
}
