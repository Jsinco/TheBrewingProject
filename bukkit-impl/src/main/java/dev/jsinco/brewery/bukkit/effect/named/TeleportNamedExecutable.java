package dev.jsinco.brewery.bukkit.effect.named;

import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.EventStepProperty;
import dev.jsinco.brewery.api.event.NamedDrunkEvent;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.util.BukkitMessageUtil;
import dev.jsinco.brewery.bukkit.util.LocationUtil;
import dev.jsinco.brewery.configuration.EventSection;
import dev.jsinco.brewery.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TeleportNamedExecutable implements EventPropertyExecutable {


    @Override
    public @NonNull ExecutionResult execute(UUID contextPlayer, List<EventStepProperty> eventStepProperties) {
        Player player = Bukkit.getPlayer(contextPlayer);
        if (player == null) {
            return ExecutionResult.CONTINUE;
        }
        List<BreweryLocation> locations = EventSection.events()
                .teleportDestinations()
                .stream()
                .flatMap(uncompiledLocation ->
                        uncompiledLocation.stream(LocationUtil::resolveWorld)
                ).filter(Objects::nonNull)
                .toList();
        if (locations.isEmpty()) {
            return ExecutionResult.CONTINUE;
        }
        BreweryLocation teleport = locations.get(RANDOM.nextInt(locations.size()));
        BukkitAdapter.toLocation(teleport)
                .ifPresent(location -> {
                    location.setPitch(player.getPitch());
                    location.setYaw(player.getYaw());
                    if (!EventSection.events().ensureSafeLocation()) {
                        teleport(player, location);
                        return;
                    }
                    int radius = EventSection.events().randomOffsetRadius();
                    int offsetRadius = radius < 0 ? Bukkit.getSpawnRadius() : radius;
                    Plugin plugin = TheBrewingProject.getInstance();
                    Bukkit.getRegionScheduler().run(plugin, location, ignored -> {
                        Location destination = LocationUtil.safeLocationInRadius(location, offsetRadius);
                        player.getScheduler().run(plugin, ignored2 -> teleport(player, destination), null);
                    });
                });
        return ExecutionResult.CONTINUE;
    }

    private static void teleport(Player player, Location location) {
        player.teleportAsync(location);
        MessageUtil.message(player, "tbp.events.teleport-message", BukkitMessageUtil.getPlayerTagResolver(player));
    }

    @Override
    public int priority() {
        return -1;
    }

    @Override
    public EventStepProperty toProperty() {
        return NamedDrunkEvent.fromKey("teleport");
    }

    @Override
    public ExecutionContext context() {
        return ExecutionContext.PLAYER;
    }

    @Override
    public EventPropertyExecutable withSkipPoint(@Nullable EventPropertyExecutable point) {
        return this; // NO-OP
    }
}
