package dev.jsinco.brewery.api.structure;

import dev.jsinco.brewery.api.vector.Location;

public interface SinglePositionStructure {

    /**
     * @return The position
     */
    Location position();


    /**
     * Destroy this structure and all components linked to it. Not persistent
     */
    void destroy();
}
