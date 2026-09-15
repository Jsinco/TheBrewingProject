package dev.jsinco.brewery.bukkit.effect.named;

import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.EventStepProperty;
import dev.jsinco.brewery.api.event.NamedDrunkEvent;
import dev.jsinco.brewery.bukkit.Statistics;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.configuration.EventSection;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class PukeNamedExecutable implements EventPropertyExecutable {

    public static final NamespacedKey PUKE_ITEM = new NamespacedKey("brewery", "puke");

    @Override
    public @NonNull ExecutionResult execute(UUID contextPlayer, List<EventStepProperty> eventStepProperties) {
        Player player = Bukkit.getPlayer(contextPlayer);
        if (player == null) {
            return ExecutionResult.CONTINUE;
        }
        int pukeTimeTicks = (int) EventSection.events().puke().pukeTime().durationTicks();
        PukeHandler pukeHandler = new PukeHandler(pukeTimeTicks, player);
        TheBrewingProject.getInstance().getActiveEventsRegistry().registerActiveEvent(player.getUniqueId(), NamedDrunkEvent.fromKey("puke"), pukeTimeTicks);
        player.getScheduler().runAtFixedRate(TheBrewingProject.getInstance(), pukeHandler::tick, null, 1, 1);
        return ExecutionResult.CONTINUE;
    }

    @Override
    public EventStepProperty toProperty() {
        return NamedDrunkEvent.fromKey("puke");
    }


    static class PukeHandler {
        private int countDown;
        private final Player player;

        PukeHandler(int pukingTicks, Player player) {
            Statistics.registerPukedItems(pukingTicks);
            this.countDown = pukingTicks;
            this.player = player;
        }


        public void tick(ScheduledTask task) {
            if (!player.isOnline() || countDown-- <= 0 || !FeaturesConfig.test(FeatureFlag.BREW_EFFECTS, player.getWorld().getName())) {
                task.cancel();
                return;
            }
            Location loc = player.getLocation();
            loc.setY(loc.getY() + 1.1);
            loc.setPitch(player.getPitch() + RANDOM.nextInt(-10, 11));
            loc.setYaw(player.getYaw() + RANDOM.nextInt(-10, 11));
            Vector direction = loc.getDirection();
            direction.multiply(0.5);
            loc.add(direction);
            Item item = player.getWorld().dropItem(loc, new ItemStack(Material.SOUL_SAND));
            item.setVelocity(direction);
            item.setPersistent(false);
            item.setCanPlayerPickup(false);
            item.setCanMobPickup(false);
            item.setPickupDelay(32767);
            item.getPersistentDataContainer().set(PUKE_ITEM, PersistentDataType.BOOLEAN, true);

            World world = loc.getWorld();

            @SuppressWarnings("removal")
            YamlConfiguration spigotConfig = Bukkit.spigot().getConfig(); // Deprecated but no obvious replacement by paper yet?
            int worldDespawnRate = spigotConfig.getInt("world-settings." + world.getName() + ".item-despawn-rate", -1);
            if (worldDespawnRate < 0) {
                worldDespawnRate = spigotConfig.getInt("world-settings.default.item-despawn-rate", 6000);
            }
            int despawnRate = Math.max((int) EventSection.events().puke().pukeDespawnTime().durationTicks(), 4);
            item.setTicksLived(worldDespawnRate - despawnRate + RANDOM.nextInt(-despawnRate / 2, despawnRate / 2 + 1));
        }
    }

    @Override
    public int priority() {
        return -1;
    }

    @Override
    public EventPropertyExecutable withSkipPoint(@Nullable EventPropertyExecutable point) {
        return this; // NO-OP
    }
}
