package dev.jsinco.brewery.bukkit.api.vector;

import com.google.common.base.Objects;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.api.vector.Location;
import dev.jsinco.brewery.api.vector.Vector;
import org.bukkit.block.Block;

import java.util.UUID;

/**
 * Util class to avoid excessive memory use
 *
 * @param block A block instance to wrap
 */
public record BlockLocationWrapper(Block block) implements Location {
    @Override
    public int x() {
        return block.getX();
    }

    @Override
    public int y() {
        return block.getY();
    }

    @Override
    public int z() {
        return block.getZ();
    }

    @Override
    public UUID worldUuid() {
        return block.getWorld().getUID();
    }

    @Override
    public Vector toVector() {
        return new BlockVectorWrapper(block);
    }

    @Override
    public BreweryLocation add(int x, int y, int z) {
        return new BreweryLocation(this.x() + x, this.y() + y, this.z() + z, worldUuid());
    }

    @Override
    public BreweryLocation add(Vector breweryVector) {
        return new BreweryLocation(this.x() + breweryVector.x(), this.y() + breweryVector.y(), this.z() + breweryVector.z(), worldUuid());
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
