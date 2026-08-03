package dev.jsinco.brewery.bukkit.effect.named;

import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.EventStepProperty;
import dev.jsinco.brewery.api.event.NamedDrunkEvent;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.effect.DrunkenImpulse;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class StumbleNamedExecutable implements EventPropertyExecutable {

    private static final int STUMBLE_DURATION = 10;
    private static final double MAX_MAGNITUDE = 0.1;
    private static final double MAX_TURN_RATE = Math.PI / 20.0;

    @Override
    public @NonNull ExecutionResult execute(UUID contextPlayer, List<EventStepProperty> eventStepProperties) {
        Player player = Bukkit.getPlayer(contextPlayer);
        if (player == null) {
            return ExecutionResult.CONTINUE;
        }

        int duration = RANDOM.nextInt(STUMBLE_DURATION / 2, STUMBLE_DURATION * 3 / 2 + 1);
        StumbleHandler stumbleHandler = new StumbleHandler(duration, player);
        TheBrewingProject.getInstance().getActiveEventsRegistry().registerActiveEvent(player.getUniqueId(), NamedDrunkEvent.fromKey("stumble"), duration);
        player.getScheduler().runAtFixedRate(TheBrewingProject.getInstance(), stumbleHandler::tick, null, 1, 1);
        return ExecutionResult.CONTINUE;
    }

    @Override
    public EventStepProperty toProperty() {
        return NamedDrunkEvent.fromKey("stumble");
    }

    static class StumbleHandler {

        private final DrunkenImpulse impulse1;
        private final DrunkenImpulse impulse2;

        private int countDown;
        private final int duration;
        private final Player player;
        private static final Random RANDOM = new Random();

        public StumbleHandler(int duration, Player player) {
            this.countDown = duration;
            this.duration = duration;
            this.player = player;
            Vector walk = TheBrewingProject.getInstance().getPlayerWalkListener().getRegisteredMovement(player.getUniqueId());
            double maxMagnitude;
            if (walk == null) {
                maxMagnitude = MAX_MAGNITUDE;
            } else {
                maxMagnitude = Math.max(MAX_MAGNITUDE, walk.length());
            }
            this.impulse1 = DrunkenImpulse.generate(RANDOM,
                    0.0, maxMagnitude,
                    0.0, MAX_TURN_RATE
            );
            this.impulse2 = DrunkenImpulse.generate(RANDOM,
                    0.0, maxMagnitude,
                    0.0, MAX_TURN_RATE
            );
        }

        public void tick(ScheduledTask task) {
            if (!player.isOnline() || player.isDead() || countDown-- < 0) {
                task.cancel();
                return;
            }
            double progress = ((double) duration - (double) countDown) / duration;
            DrunkenImpulse impulse = DrunkenImpulse.lerp(impulse1, impulse2, progress);
            impulse.applyTo(player, true);
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
