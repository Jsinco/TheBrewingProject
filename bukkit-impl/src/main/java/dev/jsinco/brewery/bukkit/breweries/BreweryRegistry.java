package dev.jsinco.brewery.bukkit.breweries;

import dev.jsinco.brewery.api.breweries.InventoryAccessible;
import dev.jsinco.brewery.api.structure.SinglePositionStructure;
import dev.jsinco.brewery.api.structure.StructureType;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.api.vector.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class BreweryRegistry {

    private final Map<Location, SinglePositionStructure> activeSingleBlockStructures = new ConcurrentHashMap<>();
    private final Map<StructureType<?>, Set<InventoryAccessible<ItemStack, Inventory>>> opened = new HashMap<>();
    private final Map<Inventory, InventoryAccessible<ItemStack, Inventory>> inventories = new ConcurrentHashMap<>();

    public Optional<SinglePositionStructure> getActiveSinglePositionStructure(Location position) {
        return Optional.ofNullable(activeSingleBlockStructures.get(position));
    }

    public void addActiveSinglePositionStructure(SinglePositionStructure cauldron) {
        activeSingleBlockStructures.put(cauldron.position(), cauldron);
    }

    public synchronized void removeActiveSinglePositionStructure(SinglePositionStructure cauldron) {
        activeSingleBlockStructures.remove(cauldron.position());
    }

    public Collection<SinglePositionStructure> getActiveSinglePositionStructure() {
        return activeSingleBlockStructures.values();
    }

    public <H extends InventoryAccessible<ItemStack, Inventory>> void registerOpened(H holder) {
        StructureType<H> structureType = getStructureType(holder);
        synchronized (opened) {
            opened.computeIfAbsent(structureType, ignored -> new HashSet<>()).add(holder);
        }
    }

    public <H extends InventoryAccessible<ItemStack, Inventory>> void unregisterOpened(H holder) {
        StructureType<H> structureType = getStructureType(holder);
        synchronized (opened) {
            opened.computeIfAbsent(structureType, ignored -> new HashSet<>()).remove(holder);
        }
    }

    private <H> StructureType<H> getStructureType(H holder) {
        for (StructureType<?> structureType : dev.jsinco.brewery.api.util.BreweryRegistry.STRUCTURE_TYPE.values()) {
            if (structureType.tClass().isInstance(holder)) {
                return (StructureType<H>) structureType;
            }
        }
        throw new IllegalArgumentException("Holder does not have a matching structure type!");
    }

    public @Nullable InventoryAccessible<ItemStack, Inventory> getFromInventory(Inventory inventory) {
        return inventories.get(inventory);
    }

    public synchronized void registerInventory(InventoryAccessible<ItemStack, Inventory> inventoryAccessible) {
        inventoryAccessible.getInventories().forEach(inventory -> inventories.put(inventory, inventoryAccessible));
    }

    public synchronized void registerInventories(List<? extends InventoryAccessible<ItemStack, Inventory>> inventoryAccessible) {
        inventoryAccessible.forEach(this::registerInventory);
    }

    public synchronized void unregisterInventory(InventoryAccessible<ItemStack, Inventory> inventoryAccessible) {
        inventoryAccessible.getInventories().forEach(inventories::remove);
    }

    public void clear() {
        activeSingleBlockStructures.forEach((ignored, structure) -> structure.destroy());
        activeSingleBlockStructures.clear();
        synchronized (opened) {
            opened.clear();
        }
        inventories.clear();
    }

    public <T> void iterate(StructureType<T> type, Consumer<T> inventoryAccessibleAction) {
        synchronized (opened) {
            Set<InventoryAccessible<ItemStack, Inventory>> inventoryAccessible = opened.get(type);
            if (inventoryAccessible == null) {
                return;
            }
            inventoryAccessible.stream()
                    .map(type.tClass()::cast)
                    .forEach(inventoryAccessibleAction);
        }
    }

    public int countOpened(StructureType<?> type) {
        synchronized (opened) {
            return opened.get(type).size();
        }
    }
}
