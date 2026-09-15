package dev.jsinco.brewery.api.breweries;

import dev.jsinco.brewery.api.structure.MultiblockStructure;
import dev.jsinco.brewery.api.structure.StructureType;
import dev.jsinco.brewery.api.vector.Location;

public interface StructureHolder<H extends StructureHolder<H>> {

    /**
     * @return The structure this is linked to
     */
    MultiblockStructure<H> getStructure();

    /**
     * Persistently destroy the structure, dropping all contained items
     *
     * @param breweryLocation The location to destroy from
     */
    void destroy(Location breweryLocation);

    /**
     * @return The type of the structure
     */
    StructureType<?> getStructureType();
}
