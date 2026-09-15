package dev.jsinco.brewery.api.vector;

import java.util.UUID;

public interface Location {
    /**
     * @return x-position
     */
    int x();

    /**
     * @return y-position
     */
    int y();

    /**
     * @return z-position
     */
    int z();

    /**
     * @return world uuid
     */
    UUID worldUuid();

    /**
     * @return The location converted to a vector
     */
    Vector toVector();

    /**
     * @param x X to add
     * @param y Y to add
     * @param z Z to add
     * @return A new brewery location with modified coordinates
     */
    BreweryLocation add(int x, int y, int z);

    /**
     *
     * @param breweryVector vector to add
     * @return A new brewery location with the modified coordinates
     */
    BreweryLocation add(Vector breweryVector);
}
