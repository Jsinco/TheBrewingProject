package dev.jsinco.brewery.bukkit.api.event.structure;

import dev.jsinco.brewery.api.breweries.Cauldron;
import dev.jsinco.brewery.api.util.CancelState;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fires when a player inserts or removes an item from a cauldron.
 */
public class CauldronAccessEvent extends StructureAccessEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Cauldron cauldron;

    public CauldronAccessEvent(CancelState state, Player player, Block block, Cauldron cauldron) {
        super(state, player, block);
        this.cauldron = cauldron;
    }

    /**
     * @return The cauldron the player accessed
     */
    public Cauldron getCauldron() {
        return cauldron;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
