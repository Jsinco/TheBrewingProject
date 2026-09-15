package dev.jsinco.brewery.bukkit.listener;

import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.breweries.BarrelType;
import dev.jsinco.brewery.api.breweries.Cauldron;
import dev.jsinco.brewery.api.breweries.InventoryAccessible;
import dev.jsinco.brewery.api.breweries.StructureHolder;
import dev.jsinco.brewery.api.structure.MultiblockStructure;
import dev.jsinco.brewery.api.structure.SinglePositionStructure;
import dev.jsinco.brewery.api.structure.StructureMeta;
import dev.jsinco.brewery.api.structure.StructureType;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.CancelState;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.api.event.structure.BarrelCreateEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.BarrelDestroyEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.CauldronDestroyEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.DistilleryCreateEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.DistilleryDestroyEvent;
import dev.jsinco.brewery.bukkit.breweries.BreweryRegistry;
import dev.jsinco.brewery.bukkit.breweries.BukkitCauldron;
import dev.jsinco.brewery.bukkit.breweries.barrel.BukkitBarrel;
import dev.jsinco.brewery.bukkit.breweries.distillery.BukkitDistillery;
import dev.jsinco.brewery.bukkit.database.SessionTypes;
import dev.jsinco.brewery.bukkit.structure.BreweryStructure;
import dev.jsinco.brewery.bukkit.structure.PlacedBreweryStructure;
import dev.jsinco.brewery.bukkit.structure.StructureRegistry;
import dev.jsinco.brewery.bukkit.util.LocationUtil;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import dev.jsinco.brewery.database.PersistenceException;
import dev.jsinco.brewery.database.sql.SqlDatabase;
import dev.jsinco.brewery.structure.PlacedStructureRegistryImpl;
import dev.jsinco.brewery.util.MessageUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockEventListener implements Listener {

    private final StructureRegistry structureRegistry;
    private final PlacedStructureRegistryImpl placedStructureRegistry;
    private final SqlDatabase database;
    private final BreweryRegistry breweryRegistry;

    public BlockEventListener(StructureRegistry structureRegistry, PlacedStructureRegistryImpl placedStructureRegistry, SqlDatabase database, BreweryRegistry breweryRegistry) {
        this.structureRegistry = structureRegistry;
        this.placedStructureRegistry = placedStructureRegistry;
        this.database = database;
        this.breweryRegistry = breweryRegistry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChangeEvent(SignChangeEvent event) {
        if (!FeaturesConfig.test(FeatureFlag.BARRELS, event.getBlock().getWorld().getName())) {
            return;
        }
        Set<String> keywords = Config.config().barrels().signKeywords().stream().map(String::toLowerCase).collect(Collectors.toSet());
        String firstLine = PlainTextComponentSerializer.plainText().serialize(event.lines().getFirst()).toLowerCase();
        if (Config.config().barrels().requireSignKeyword() && !keywords.contains(firstLine)) {
            return;
        }

        if (!(event.getBlock().getBlockData() instanceof WallSign wallSign)) {
            return;
        }
        Optional<Pair<PlacedBreweryStructure<BukkitBarrel>, BarrelType>> possibleStructure = getBarrel(event.getBlock().getRelative(wallSign.getFacing().getOppositeFace()));
        if (possibleStructure.isEmpty()) {
            return;
        }
        Pair<PlacedBreweryStructure<BukkitBarrel>, BarrelType> placedStructurePair = possibleStructure.get();
        PlacedBreweryStructure<BukkitBarrel> placedBreweryStructure = placedStructurePair.first();
        if (!placedStructureRegistry.getStructures(placedBreweryStructure.positions()).isEmpty()) {
            // Exit if there's an overlapping structure
            return;
        }
        BukkitBarrel barrel = new BukkitBarrel(BukkitAdapter.toLocation(placedBreweryStructure.getUnique()).orElseThrow(), placedBreweryStructure, placedBreweryStructure.getStructure().getMeta(StructureMeta.INVENTORY_SIZE), placedStructurePair.second());
        BarrelCreateEvent barrelCreateEvent = new BarrelCreateEvent(
                event.getPlayer().hasPermission("brewery.barrel.create") ?
                        new CancelState.Allowed() : new CancelState.PermissionDenied(Component.translatable("tbp.barrel.create-denied")),
                event.getPlayer(),
                event.getBlock().getLocation(),
                barrel);
        if (!barrelCreateEvent.callEvent()) {
            barrelCreateEvent.getCancelState().sendMessage(event.getPlayer());
            return;
        }
        MessageUtil.message(event.getPlayer(), "tbp.barrel.create");
        placedBreweryStructure.setHolder(barrel);
        placedStructureRegistry.registerStructure(placedBreweryStructure);
        breweryRegistry.registerInventory(barrel);
        try {
            database.startSession(SessionTypes.BARREL_SESSION_TYPE).insertBarrel(barrel)
                    .exceptionally(Logger::logAndTrackErr);
        } catch (PersistenceException e) {
            Logger.logErr(e);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent placeEvent) {
        if (!FeaturesConfig.test(FeatureFlag.DISTILLERIES, placeEvent.getBlock().getWorld().getName())) {
            return;
        }
        Block placed = placeEvent.getBlockPlaced();
        for (BreweryStructure breweryStructure : structureRegistry.getPossibleStructures(placed.getType().asBlockType(), StructureType.DISTILLERY)) {
            Optional<Pair<PlacedBreweryStructure<BukkitDistillery>, BreweryKey>> placedBreweryStructureOptional = PlacedBreweryStructure.findValid(
                    breweryStructure,
                    placed.getLocation()
            );
            if (placedBreweryStructureOptional.isPresent()) {
                if (!placedStructureRegistry.getStructures(placedBreweryStructureOptional.get().first().positions()).isEmpty()) {
                    continue;
                }

                Player player = placeEvent.getPlayer();
                BukkitDistillery bukkitDistillery = new BukkitDistillery(placedBreweryStructureOptional.get().first());
                DistilleryCreateEvent createEvent = new DistilleryCreateEvent(player.hasPermission("brewery.distillery.create") ?
                        new CancelState.Allowed() : new CancelState.PermissionDenied(Component.translatable("tbp.distillery.create-denied")),
                        player,
                        placeEvent.getBlockPlaced().getLocation(),
                        bukkitDistillery
                );
                if (!createEvent.callEvent()) {
                    createEvent.getCancelState().sendMessage(placeEvent.getPlayer());
                    return;
                }
                registerDistillery(placedBreweryStructureOptional.get().first(), bukkitDistillery);
                MessageUtil.message(player, "tbp.distillery.create");
                return;
            }
        }
    }

    private void registerDistillery(PlacedBreweryStructure<BukkitDistillery> distilleryPlacedBreweryStructure, BukkitDistillery bukkitDistillery) {
        distilleryPlacedBreweryStructure.setHolder(bukkitDistillery);
        placedStructureRegistry.registerStructure(distilleryPlacedBreweryStructure);
        try {
            database.startSession(SessionTypes.DISTILLERY_SESSION_TYPE).insertDistillery(bukkitDistillery)
                    .exceptionally(Logger::logAndTrackErr);
            breweryRegistry.registerInventory(bukkitDistillery);
        } catch (PersistenceException e) {
            Logger.logErr(e);
        }
    }

    private Optional<Pair<PlacedBreweryStructure<BukkitBarrel>, BarrelType>> getBarrel(Block block) {
        Location placedLocation = block.getLocation();
        Material material = block.getType();
        Set<BreweryStructure> possibleStructures = structureRegistry.getPossibleStructures(material.asBlockType(), StructureType.BARREL);
        for (BreweryStructure structure : possibleStructures) {
            Optional<Pair<PlacedBreweryStructure<BukkitBarrel>, BreweryKey>> placedBreweryStructure = PlacedBreweryStructure.findValid(
                    structure,
                    placedLocation
            );
            if (placedBreweryStructure.isPresent() && dev.jsinco.brewery.api.util.BreweryRegistry.BARREL_TYPE.containsKey(placedBreweryStructure.get().second())) {
                return placedBreweryStructure.map(pair -> pair.mapSecond(dev.jsinco.brewery.api.util.BreweryRegistry.BARREL_TYPE::get));
            }
        }
        return Optional.empty();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        boolean success = destroyFromBlock(event.getBlock(), event.getPlayer());
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        boolean success = onMultiBlockRemove(event.getBlocks().stream()
                .map(Block::getLocation)
                .toList(), null);
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        boolean success = onMultiBlockRemove(event.getBlocks().stream()
                .map(Block::getLocation)
                .toList(), null);
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getExplosionResult() == ExplosionResult.TRIGGER_BLOCK) {
            return;
        }
        boolean success = onMultiBlockRemove(event.blockList().stream()
                .map(Block::getLocation)
                .toList(), null);
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.getExplosionResult() == ExplosionResult.TRIGGER_BLOCK) {
            return;
        }
        boolean success = onMultiBlockRemove(event.blockList().stream()
                .map(Block::getLocation)
                .toList(), null);
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        boolean success = destroyFromBlock(event.getBlock(), null);
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Player player = event.getEntity() instanceof Player p ? p : null;
        boolean success = destroyFromBlock(event.getBlock(), player);
        if (!success) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperInventorySearch(HopperInventorySearchEvent event) {
        Block searchBlock = event.getSearchBlock();
        BreweryLocation breweryLocation = BukkitAdapter.toBreweryLocation(searchBlock);
        Optional<InventoryAccessible<ItemStack, Inventory>> inventoryAccessibleOptional = placedStructureRegistry.getStructure(breweryLocation)
                .map(MultiblockStructure::getHolder)
                .filter(InventoryAccessible.class::isInstance)
                .map(inventoryAccessible -> (InventoryAccessible<ItemStack, Inventory>) inventoryAccessible);
        if (!Config.config().automation()) {
            inventoryAccessibleOptional.ifPresent(ignored -> event.setInventory(null));
            return;
        }
        inventoryAccessibleOptional
                .flatMap(inventoryAccessible -> inventoryAccessible.access(breweryLocation))
                .ifPresent(event::setInventory);
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        BreweryLocation breweryLocation = BukkitAdapter.toBreweryLocation(event.getBlock());
        breweryRegistry.getActiveSinglePositionStructure(breweryLocation)
                .filter(BukkitCauldron.class::isInstance)
                .map(BukkitCauldron.class::cast)
                .ifPresent(bukkitCauldron -> bukkitCauldron.updateLevel(event.getNewState().getBlockData()));
    }

    /**
     * Assumes only one block has changed in the event, is not safe to use in multi-block changes
     *
     * @param block
     */
    private boolean destroyFromBlock(Block block, @Nullable Player player) {
        return onMultiBlockRemove(List.of(block.getLocation()), player);
    }

    private boolean onMultiBlockRemove(List<Location> locations, @Nullable Player player) {
        if (locations.isEmpty()) {
            return true;
        }
        if (!FeaturesConfig.test(FeatureFlag.BREW_MAKING, locations.getFirst().getWorld().getName())) {
            return true;
        }
        Set<SinglePositionStructure> singlePositionStructures = new HashSet<>();
        Set<MultiblockStructure<?>> multiblockStructures = new HashSet<>();
        Map<StructureHolder<?>, List<Brew>> holdersToDrops = new HashMap<>();

        for (Location location : locations) {
            BreweryLocation breweryLocation = BukkitAdapter.toBreweryLocation(location);
            Optional<SinglePositionStructure> single = breweryRegistry.getActiveSinglePositionStructure(breweryLocation)
                    .filter(singlePositionStructure -> !singlePositionStructures.contains(singlePositionStructure));
            CancelState cancelState = single
                    .map(singlePositionStructure -> callSinglePositionStructureEvent(location, player, singlePositionStructure))
                    .orElseGet(CancelState.Allowed::new);
            if (actOnCancelState(cancelState, player)) {
                return false;
            }
            single.ifPresent(singlePositionStructures::add);

            Optional<StructureHolder<?>> structureHolderOptional = placedStructureRegistry.getHolder(breweryLocation)
                    .filter(structureHolder -> !multiblockStructures.contains(structureHolder.getStructure()));
            Result result = structureHolderOptional
                    .map(holder -> callPlacedStructureEvent(location, player, holder))
                    .orElseGet(() -> new Result(new CancelState.Allowed(), List.of()));
            if (actOnCancelState(result.cancelState(), player)) {
                return false;
            }
            structureHolderOptional.ifPresent(holder -> {
                holdersToDrops.put(holder, result.drops);
                multiblockStructures.add(holder.getStructure());
            });
        }

        singlePositionStructures.forEach(ListenerUtil::removeActiveSinglePositionStructure);
        multiblockStructures.forEach(placedStructureRegistry::unregisterStructure);
        Location location = locations.getFirst();
        for (Map.Entry<StructureHolder<?>, List<Brew>> entry : holdersToDrops.entrySet()) {
            StructureHolder<?> holder = entry.getKey();
            List<Brew> drops = entry.getValue();
            if (holder instanceof InventoryAccessible inventoryAccessible) {
                breweryRegistry.unregisterInventory(inventoryAccessible);
            }
            remove(holder);
            LocationUtil.dropBrews(location, drops);
        }
        return true;
    }

    /**
     *
     * @return True if cancel state means to cancel
     */
    private static boolean actOnCancelState(CancelState cancelState, @Nullable Audience audience) {
        if (cancelState instanceof CancelState.PermissionDenied permissionDenied) {
            permissionDenied.sendMessage(audience);
            return true;
        }
        return cancelState instanceof CancelState.Cancelled;
    }

    private static CancelState callSinglePositionStructureEvent(Location location, @Nullable Player player, SinglePositionStructure structure) {
        if (structure instanceof Cauldron cauldron) {
            CauldronDestroyEvent event = new CauldronDestroyEvent(
                    player == null || player.hasPermission("brewery.cauldron.access") ?
                            new CancelState.Allowed() :
                            new CancelState.PermissionDenied(Component.translatable("tbp.cauldron.access-denied")),
                    cauldron,
                    player,
                    location
            );
            event.callEvent();
            return event.getCancelState();
        }
        return new CancelState.Allowed();
    }

    private static Result callPlacedStructureEvent(Location location, @Nullable Player player, StructureHolder<?> holder) {
        return switch (holder) {
            case BukkitBarrel barrel -> {
                BarrelDestroyEvent event = new BarrelDestroyEvent(
                        player == null || player.hasPermission("brewery.barrel.access") ?
                                new CancelState.Allowed() :
                                new CancelState.PermissionDenied(Component.translatable("tbp.barrel.access-denied")),
                        barrel,
                        player,
                        location,
                        barrel.calculateDestroyDrops()
                );
                event.callEvent();
                yield new Result(event.getCancelState(), event.getDrops());
            }
            case BukkitDistillery distillery -> {
                DistilleryDestroyEvent event = new DistilleryDestroyEvent(
                        player == null || player.hasPermission("brewery.distillery.access") ?
                                new CancelState.Allowed() :
                                new CancelState.PermissionDenied(Component.translatable("tbp.distillery.access-denied")),
                        distillery,
                        player,
                        location,
                        distillery.calculateDestroyDrops()
                );
                event.callEvent();
                yield new Result(event.getCancelState(), event.getDrops());
            }
            default -> new Result(new CancelState.Allowed(), Collections.emptyList());
        };
    }

    private record Result(CancelState cancelState, List<Brew> drops) {
    }

    private void remove(StructureHolder<?> holder) {
        try {
            switch (holder) {
                case BukkitBarrel barrel -> {
                    barrel.destroyWithoutDrops();
                    database.startSession(SessionTypes.BARREL_SESSION_TYPE).removeBarrel(barrel)
                            .exceptionally(Logger::logAndTrackErr);
                }
                case BukkitDistillery distillery -> {
                    distillery.destroyWithoutDrops();
                    database.startSession(SessionTypes.DISTILLERY_SESSION_TYPE).removeDistillery(distillery)
                            .exceptionally(Logger::logAndTrackErr);
                }
                default -> throw new IllegalArgumentException("Unknown structure type");
            }
        } catch (PersistenceException e) {
            Logger.logErr(e);
        }
    }
}
