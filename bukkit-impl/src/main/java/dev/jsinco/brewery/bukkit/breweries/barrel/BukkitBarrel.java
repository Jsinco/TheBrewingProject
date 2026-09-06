package dev.jsinco.brewery.bukkit.breweries.barrel;

import com.google.common.base.Preconditions;
import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.brew.BrewingStep;
import dev.jsinco.brewery.api.breweries.Barrel;
import dev.jsinco.brewery.api.breweries.BarrelAccess;
import dev.jsinco.brewery.api.breweries.BarrelType;
import dev.jsinco.brewery.api.breweries.BrewInventory;
import dev.jsinco.brewery.api.moment.Interval;
import dev.jsinco.brewery.api.moment.Moment;
import dev.jsinco.brewery.api.util.CancelState;
import dev.jsinco.brewery.api.util.Holder;
import dev.jsinco.brewery.api.util.HolderProviderHolder;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.brew.AgeStepImpl;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.api.event.process.BrewAgeEvent;
import dev.jsinco.brewery.bukkit.api.event.structure.BarrelAccessEvent;
import dev.jsinco.brewery.bukkit.brew.BrewAdapterAccess;
import dev.jsinco.brewery.bukkit.breweries.BrewInventoryImpl;
import dev.jsinco.brewery.bukkit.structure.PlacedBreweryStructure;
import dev.jsinco.brewery.bukkit.util.LocationUtil;
import dev.jsinco.brewery.bukkit.util.SoundPlayer;
import dev.jsinco.brewery.configuration.Config;
import dev.jsinco.brewery.util.MessageUtil;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BukkitBarrel implements Barrel<BukkitBarrel, ItemStack, Inventory>, BarrelAccess {
    private final PlacedBreweryStructure<BukkitBarrel> structure;
    private final int size;
    private final BarrelType type;
    private final Location uniqueLocation;
    private final BrewInventoryImpl inventory;
    private long recentlyAccessed = -1L;
    private long ticksUntilNextCheck = 0L;
    private static final Random RANDOM = new Random();

    public BukkitBarrel(Location uniqueLocation, @NonNull PlacedBreweryStructure<BukkitBarrel> structure, int size, @NonNull BarrelType type) {
        this.structure = Preconditions.checkNotNull(structure);
        this.size = size;
        this.type = Preconditions.checkNotNull(type);
        this.uniqueLocation = Preconditions.checkNotNull(uniqueLocation);
        this.inventory = new BrewInventoryImpl(Component.translatable("tbp.barrel.gui-title"), size, new BarrelBrewPersistenceHandler(BukkitAdapter.toBreweryLocation(uniqueLocation)));
    }

    @Override
    public CancelState open(@NonNull BreweryLocation location, Holder.@NonNull Player playerHolder) {
        Player player = BukkitAdapter.toPlayer(playerHolder)
                .orElse(null);
        if (player == null) {
            return new CancelState.Cancelled();
        }
        if (!player.hasPermission("brewery.barrel.access")) {
            return new CancelState.PermissionDenied(Component.translatable("tbp.barrel.access-denied"));
        }
        if (inventoryUnpopulated()) {
            inventory.updateInventoryFromBrews();
        }
        recentlyAccessed = TheBrewingProject.getInstance().getTime();
        if (uniqueLocation != null) {
            SoundPlayer.playSoundEffect(Config.config().sounds().barrelOpen(), Sound.Source.BLOCK, uniqueLocation.toCenterLocation());
        }
        player.openInventory(inventory.getInventory());
        return new CancelState.Allowed();
    }

    @Override
    public boolean inventoryAllows(@NonNull UUID playerUuid, @NonNull ItemStack item) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return false;
        }
        if (!player.hasPermission("brewery.barrel.access")) {
            MessageUtil.message(player, "tbp.barrel.access-denied");
            return false;
        }
        return inventoryAllows(item);
    }

    @Override
    public boolean inventoryAllows(@NonNull ItemStack item) {
        return BrewAdapterAccess.fromItem(item).isPresent();
    }

    @Override
    public Set<Inventory> getInventories() {
        return Set.of(this.inventory.getInventory());
    }

    @Override
    public boolean open(@NonNull BreweryLocation breweryLocation, @NonNull UUID playerUuid) {
        Optional<Holder.Player> playerOptional = HolderProviderHolder.instance().player(playerUuid);
        if (playerOptional.isEmpty()) {
            return false;
        }
        Holder.Player playerHolder = playerOptional.get();
        CancelState cancelState = open(breweryLocation, playerHolder);
        Player player = BukkitAdapter.toPlayer(playerHolder).orElse(null);
        Block block = BukkitAdapter.toBlock(breweryLocation).orElse(null);
        if (player != null && block != null) {
            BarrelAccessEvent event = new BarrelAccessEvent(cancelState, player, block, this);
            event.callEvent();
            cancelState = event.getCancelState();
        }
        return switch (cancelState) {
            case CancelState.Cancelled ignored -> false;
            case CancelState.Allowed ignored -> true;
            case CancelState.PermissionDenied(Component message) -> {
                if (player != null) {
                    player.sendMessage(message);
                }
                yield false;
            }
        };
    }

    public void close(boolean silent) {
        this.ticksUntilNextCheck = 0L;
        Brew[] previousBrews = Arrays.copyOf(inventory.getBrews(), inventory.getBrews().length);
        this.inventory.updateBrewsFromInventory();
        processBrews(previousBrews);
        if (!silent && uniqueLocation != null) {
            SoundPlayer.playSoundEffect(Config.config().sounds().barrelClose(), Sound.Source.BLOCK, uniqueLocation.toCenterLocation());
        }
        this.inventory.getInventory().clear();
    }

    @Override
    public void tickInventory() {
        if (recentlyAccessed == -1L) {
            return;
        }
        if (shouldUnpopulateInventory()) {
            close(false);
            Bukkit.getAsyncScheduler().runNow(TheBrewingProject.getInstance(), ignored ->
                    TheBrewingProject.getInstance().getBreweryRegistry().unregisterOpened(this)
            );
            recentlyAccessed = -1L;
            return;
        }
        if (!inventory.getInventory().getViewers().isEmpty()) {
            this.recentlyAccessed = TheBrewingProject.getInstance().getTime();
        }
        if (ticksUntilNextCheck-- > 0L) {
            return;
        }
        ticksUntilNextCheck = Math.min(Config.config().barrels().agingYearTicks() / 20L, Moment.MINUTE * 5);
        Brew[] previousBrews = Arrays.copyOf(inventory.getBrews(), inventory.getBrews().length);
        inventory.updateBrewsFromInventory();
        processBrews(previousBrews);
        inventory.updateInventoryFromBrews();
        getInventory().getInventory().getViewers()
                .stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .forEach(Player::updateInventory);
    }

    private void processBrews(Brew[] previousBrews) {
        Brew[] brews = inventory.getBrews();
        long time = TheBrewingProject.getInstance().getTime();
        for (int i = 0; i < brews.length; i++) {
            Brew brew = brews[i];
            if (brew == null) {
                continue;
            }
            final int idx = i;
            if (!(brew.lastStep() instanceof BrewingStep.Age age) || age.barrelType() != type) {
                Brew aged = brew.withStep(new AgeStepImpl(new Interval(time, time), this.type));
                callAgeEvent(brew, aged).ifPresent(result -> inventory.store(result, idx));
                continue;
            }
            Brew aged;
            if (Objects.equals(previousBrews[i], brew)) {
                aged = brew.withLastStep(BrewingStep.Age.class,
                        age1 -> age1.withAge(age.time().withLastStep(time)),
                        () -> new AgeStepImpl(new Interval(time, time), this.type));
            } else {
                aged = brew.withLastStep(BrewingStep.Age.class,
                        age1 -> age1.withAge(age.time().withMovedEnding(time)),
                        () -> new AgeStepImpl(new Interval(time, time), this.type));
            }
            callAgeEvent(brew, aged).ifPresent(result -> brews[idx] = result);

        }
    }

    private Optional<Brew> callAgeEvent(Brew source, Brew result) {
        BrewAgeEvent event = new BrewAgeEvent(this, source, result);
        if (!event.callEvent()) {
            return Optional.empty();
        }
        return Optional.of(event.getResult());
    }

    @Override
    public Optional<Inventory> access(@NonNull BreweryLocation breweryLocation) {
        if (inventoryUnpopulated()) {
            inventory.updateInventoryFromBrews();
            TheBrewingProject.getInstance().getBreweryRegistry().registerOpened(this);
        }
        this.recentlyAccessed = TheBrewingProject.getInstance().getTime();
        return Optional.of(inventory.getInventory());
    }

    @Override
    public Brew initializeBrew(Brew brew) {
        long time = TheBrewingProject.getInstance().getTime();
        if (!(brew.lastStep() instanceof BrewingStep.Age age) || age.barrelType() != type) {
            return brew.withStep(new AgeStepImpl(new Interval(time, time), this.type));
        }
        return brew;
    }

    /**
     * Ensures that the barrel's inventory is up-to-date before the barrel is destroyed.
     *
     * @return A snapshot of the brews that should drop from the barrel
     */
    public List<Brew> calculateDestroyDrops() {
        if (!inventoryUnpopulated()) {
            inventory.updateBrewsFromInventory();
        }
        return inventory.getBrewSnapshot();
    }

    public void destroyWithoutDrops() {
        inventory.destroy();
    }

    @Override
    public void destroy(BreweryLocation breweryLocation) {
        calculateDestroyDrops();
        LocationUtil.dropBrews(breweryLocation, inventory.destroy());
    }

    @Override
    public BrewInventory getBrewInventory() {
        return inventory;
    }

    @Override
    public PlacedBreweryStructure<BukkitBarrel> getStructure() {
        return structure;
    }

    public World getWorld() {
        return uniqueLocation.getWorld();
    }

    public List<Pair<Brew, Integer>> getBrews() {
        List<Pair<Brew, Integer>> brewList = new ArrayList<>();
        for (int i = 0; i < inventory.getBrews().length; i++) {
            if (inventory.getBrews()[i] == null) {
                continue;
            }
            brewList.add(new Pair<>(inventory.getBrews()[i], i));
        }
        return brewList;
    }

    private boolean inventoryUnpopulated() {
        return recentlyAccessed == -1L;
    }

    private boolean shouldUnpopulateInventory() {
        return recentlyAccessed == -1L || recentlyAccessed + Moment.SECOND <= TheBrewingProject.getInstance().getTime();
    }

    public BrewInventoryImpl getInventory() {
        return this.inventory;
    }

    public long getRecentlyAccessed() {
        return this.recentlyAccessed;
    }

    public long getTicksUntilNextCheck() {
        return this.ticksUntilNextCheck;
    }

    public int getSize() {
        return this.size;
    }

    public BarrelType getType() {
        return this.type;
    }

    public Location getUniqueLocation() {
        return this.uniqueLocation;
    }

    @Override
    public CompletableFuture<Void> runLocally(Runnable action) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        Bukkit.getRegionScheduler().run(TheBrewingProject.getInstance(), getUniqueLocation(), ignored -> {
            action.run();
            completableFuture.complete(null);
        });
        return completableFuture;
    }
}
