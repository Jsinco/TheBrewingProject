package dev.jsinco.brewery.bukkit.effect.named;

import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.EventStepProperty;
import dev.jsinco.brewery.api.event.NamedDrunkEvent;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.configuration.EventSection;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class FeverNamedExecutable implements EventPropertyExecutable {

    @Override
    public @NonNull ExecutionResult execute(UUID contextPlayer, List<EventStepProperty> eventStepProperties) {
        Player player = Bukkit.getPlayer(contextPlayer);
        if (player == null || !FeaturesConfig.test(FeatureFlag.BREW_EFFECTS, player.getWorld().getName())) {
            return ExecutionResult.CONTINUE;
        }
        AtomicInteger ticksRan = new AtomicInteger(0);
        player.getScheduler().runAtFixedRate(TheBrewingProject.getInstance(), task -> {
            if (ticksRan.get() == 0) {
                player.setFireTicks((int) EventSection.events().feverBurnTime().durationTicks());
            }
            if (ticksRan.incrementAndGet() >= EventSection.events().feverFreezingTime().durationTicks()) {
                task.cancel();
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            player.setFreezeTicks(player.getMaxFreezeTicks());
        }, null, 1, 1);
        return ExecutionResult.CONTINUE;
    }

    @Override
    public EventPropertyExecutable withSkipPoint(@Nullable EventPropertyExecutable point) {
        return this; // NO-OP
    }

    @Override
    public EventStepProperty toProperty() {
        return NamedDrunkEvent.fromKey("fever");
    }


    @Override
    public int priority() {
        return -1;
    }

}
