package dev.jsinco.brewery.bukkit.api.event.structure;

import dev.jsinco.brewery.api.breweries.DistilleryAccess;
import dev.jsinco.brewery.api.util.CancelState;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fires when a player opens a distillery inventory.
 */
public class DistilleryAccessEvent extends StructureAccessEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final DistilleryAccess distillery;

    public DistilleryAccessEvent(CancelState state, Player player, Block block, DistilleryAccess distillery) {
        super(state, player, block);
        this.distillery = distillery;
    }

    /**
     * @return The distillery the player accessed
     */
    public DistilleryAccess getDistillery() {
        return distillery;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
