package dev.jsinco.brewery.bukkit.listener;

import dev.jsinco.brewery.api.breweries.InventoryAccessible;
import dev.jsinco.brewery.api.structure.MultiblockStructure;
import dev.jsinco.brewery.bukkit.api.event.transaction.ItemTransactionEvent;
import dev.jsinco.brewery.bukkit.api.transaction.ItemSource;
import dev.jsinco.brewery.bukkit.api.transaction.ItemTransaction;
import dev.jsinco.brewery.bukkit.api.vector.BlockLocationWrapper;
import dev.jsinco.brewery.bukkit.breweries.BreweryRegistry;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.configuration.EnabledState;
import dev.jsinco.brewery.structure.PlacedStructureRegistryImpl;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public record HopperEventListener(PlacedStructureRegistryImpl placedStructureRegistry,
                                  BreweryRegistry registry) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onHopperInventorySearch(HopperInventorySearchEvent event) {
        if (Config.config().automation() == EnabledState.DISABLED && event.getInventory() == null) { // Avoid unnecessary processing if no inventory is there
            return;
        }
        Block searchBlock = event.getSearchBlock();
        dev.jsinco.brewery.api.vector.Location breweryLocation = new BlockLocationWrapper(searchBlock);
        Optional<InventoryAccessible<ItemStack, Inventory>> inventoryAccessibleOptional = placedStructureRegistry.getStructure(breweryLocation)
                .map(MultiblockStructure::getHolder)
                .filter(InventoryAccessible.class::isInstance)
                .map(inventoryAccessible -> (InventoryAccessible<ItemStack, Inventory>) inventoryAccessible);
        if (Config.config().automation() == EnabledState.DISABLED) {
            inventoryAccessibleOptional.ifPresent(ignored -> event.setInventory(null));
            return;
        }
        inventoryAccessibleOptional
                .flatMap(inventoryAccessible -> inventoryAccessible.access(breweryLocation))
                .ifPresent(event::setInventory);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Optional<InventoryAccessible<ItemStack, Inventory>> source = Optional.ofNullable(registry.getFromInventory(event.getSource()));
        Optional<InventoryAccessible<ItemStack, Inventory>> destination = Optional.ofNullable(registry.getFromInventory(event.getDestination()));
        Optional<InventoryAccessible<ItemStack, Inventory>> both = destination.or(() -> source);
        if (Config.config().automation() == EnabledState.DISABLED) {
            both.ifPresent(ignored -> event.setCancelled(true));
            return;
        }
        if (both.isEmpty()) {
            return;
        }
        InventoryAccessible<ItemStack, Inventory> inventoryAccessible = both.get();
        ItemTransactionEvent<?> transactionEvent = InventoryEventListener.eventFromStructure(
                inventoryAccessible,
                new ItemTransaction.FirstInventoryPosition(source.isPresent()),
                new ItemTransaction.FirstInventoryPosition(destination.isPresent()),
                event.getItem(),
                destination.isPresent(),
                null
        );
        if (!transactionEvent.callEvent()) {
            event.setCancelled(true);
            return;
        }
        ItemSource result = transactionEvent.getTransactionSession().getResult();
        if (result == null) {
            event.setCancelled(true);
            return;
        }
        event.setItem(result.get());
    }
}
