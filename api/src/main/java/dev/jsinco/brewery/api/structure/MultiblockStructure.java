package dev.jsinco.brewery.api.structure;

import dev.jsinco.brewery.api.breweries.StructureHolder;
import dev.jsinco.brewery.api.vector.Location;

import java.util.List;

public interface MultiblockStructure<H extends StructureHolder<H>> {

    /**
     * @return The block positions of this structure
     */
    List<Location> positions();

    /**
     * @return A behavior holder
     */
    H getHolder();

    /**
     * @param holder A behavior holder
     */
    void setHolder(H holder);

    /**
     * @return A unique position to identify this structure
     */
    Location getUnique();
}
