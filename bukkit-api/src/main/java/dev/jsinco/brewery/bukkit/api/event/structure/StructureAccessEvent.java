package dev.jsinco.brewery.bukkit.api.event.structure;

import dev.jsinco.brewery.api.util.CancelState;
import dev.jsinco.brewery.bukkit.api.event.PermissibleBreweryEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public abstract class StructureAccessEvent extends PermissibleBreweryEvent {

    private final Player player;
    private final Block block;

    protected StructureAccessEvent(CancelState state, Player player, Block block) {
        super(state);
        this.player = player;
        this.block = block;
    }

    /**
     * @return The player that accessed the structure.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The block the player used to access the structure.
     */
    public Block getBlock() {
        return block;
    }

    /**
     * @return The location where the player accessed the structure.
     */
    public Location getLocation() {
        return block.getLocation();
    }

}
