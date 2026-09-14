package dev.jsinco.brewery.bukkit.api.vector;

import com.google.common.base.Objects;
import dev.jsinco.brewery.api.vector.Vector;
import org.bukkit.block.Block;

public record BlockVectorWrapper(Block block) implements Vector {
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
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Vector vector)) {
            return false;
        }
        return vector.x() == x() && vector.y() == this.y() && vector.z() == this.z();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x(), y(), z());
    }
}
