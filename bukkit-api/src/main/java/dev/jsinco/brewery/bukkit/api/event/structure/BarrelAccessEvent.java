package dev.jsinco.brewery.bukkit.api.event.structure;

import dev.jsinco.brewery.api.breweries.BarrelAccess;
import dev.jsinco.brewery.api.util.CancelState;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fires when a player opens a barrel inventory.
 */
public class BarrelAccessEvent extends StructureAccessEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BarrelAccess barrel;

    public BarrelAccessEvent(CancelState state, Player player, Block block, BarrelAccess barrel) {
        super(state, player, block);
        this.barrel = barrel;
    }

    /**
     * @return The barrel the player accessed
     */
    public BarrelAccess getBarrel() {
        return barrel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
