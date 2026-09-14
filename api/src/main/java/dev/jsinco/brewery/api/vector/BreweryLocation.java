package dev.jsinco.brewery.api.vector;

import com.google.common.base.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @param x         Position
 * @param y         Position
 * @param z         Position
 * @param worldUuid UUID of world
 */
public record BreweryLocation(int x, int y, int z, UUID worldUuid) implements Location {

    @Override
    public BreweryVector toVector() {
        return new BreweryVector(x, y, z);
    }

    /**
     * @param x X to add
     * @param y Y to add
     * @param z Z to add
     * @return A new brewery location with modified coordinates
     */
    public BreweryLocation add(int x, int y, int z) {
        return new BreweryLocation(x + x(), y + y(), z + z(), worldUuid);
    }

    /**
     *
     * @param breweryVector
     * @return A new brewery location with the modified coordinates
     */
    public BreweryLocation add(Vector breweryVector) {
        return add(breweryVector.x(), breweryVector.y(), breweryVector.z());
    }

    @ApiStatus.Internal
    public record Uncompiled(int x, int y, int z, String worldIdentifier) {

        public Optional<BreweryLocation> get(Function<String, @Nullable UUID> worldUuidFunction) {
            return Optional.ofNullable(worldUuidFunction.apply(worldIdentifier))
                    .map(worldUuid -> new BreweryLocation(x, y, z, worldUuid));
        }

        /**
         * Utility method if you want to convert the object in a stream (Stream#flatMap)
         *
         * @param worldUuidFunction
         * @return
         */
        public Stream<BreweryLocation> stream(Function<String, @Nullable UUID> worldUuidFunction) {
            return get(worldUuidFunction).stream();
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Location location)) {
            return false;
        }
        return location.x() == x() && location.y() == this.y() && location.z() == this.z() && location.worldUuid().equals(this.worldUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x(), y(), z(), worldUuid());
    }
}
