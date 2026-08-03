package dev.jsinco.brewery.bukkit.effect.named;

import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.EventStepProperty;
import dev.jsinco.brewery.api.event.NamedDrunkEvent;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.bukkit.Statistics;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.effect.DrunkenImpulse;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class DrunkenWalkNamedExecutable implements EventPropertyExecutable {

    private static final int DRUNKEN_WALK_DURATION = 400;


    @Override
    public @NonNull ExecutionResult execute(UUID contextPlayer, List<EventStepProperty> eventStepProperties) {
        Player player = Bukkit.getPlayer(contextPlayer);
        if (player == null) {
            return ExecutionResult.CONTINUE;
        }

        int duration = RANDOM.nextInt(DRUNKEN_WALK_DURATION / 2, DRUNKEN_WALK_DURATION * 3 / 2);
        DrunkenWalkHandler drunkenWalkHandler = new DrunkenWalkHandler(duration, player);
        player.getScheduler().runAtFixedRate(TheBrewingProject.getInstance(), drunkenWalkHandler::tick, null, 1, 1);
        TheBrewingProject.getInstance().getActiveEventsRegistry().registerActiveEvent(player.getUniqueId(), NamedDrunkEvent.fromKey("drunken_walk"), duration);
        return ExecutionResult.CONTINUE;
    }

    @Override
    public EventStepProperty toProperty() {
        return NamedDrunkEvent.fromKey("drunken_walk");
    }

    @Override
    public EventPropertyExecutable withSkipPoint(@Nullable EventPropertyExecutable point) {
        return this; // NO-OP
    }

    static class DrunkenWalkHandler {

        private final Queue<Pair<DrunkenImpulse, Integer>> impulses;
        private int currentTimestamp;
        private int duration;
        private int timestamp = 0;
        private final Player player;
        private DrunkenImpulse currentImpulse;
        private final Location startingPoint;

        private static final int DIRECTION_INTERVAL = 20;
        private static final double MINIMUM_PUSH_MAGNITUDE = 0.1;
        private static final double MAXIMUM_PUSH_MAGNITUDE = 0.4;
        private static final double MINIMUM_TURN_RATE = Math.PI / 40.0;
        private static final double MAXIMUM_TURN_RATE = Math.PI / 10.0;
        private static final Random RANDOM = new Random();

        DrunkenWalkHandler(int duration, Player player) {
            this.duration = duration;
            this.player = player;
            this.impulses = compileRandomImpulses(duration);
            pollNewCurrentImpulse(impulses);
            this.startingPoint = player.getLocation().clone();
        }

        private void pollNewCurrentImpulse(Queue<Pair<DrunkenImpulse, Integer>> vectors) {
            Pair<DrunkenImpulse, Integer> pair = vectors.poll();
            this.currentImpulse = pair == null ? null : pair.first();
            this.currentTimestamp = pair == null ? 0 : pair.second();
        }

        private Queue<Pair<DrunkenImpulse, Integer>> compileRandomImpulses(int duration) {
            int amount = duration / DIRECTION_INTERVAL;
            Queue<Pair<DrunkenImpulse, Integer>> output = new LinkedList<>();
            for (int i = 0; i < amount; i++) {
                DrunkenImpulse impulse = DrunkenImpulse.generate(RANDOM,
                        MINIMUM_PUSH_MAGNITUDE, MAXIMUM_PUSH_MAGNITUDE,
                        MINIMUM_TURN_RATE, MAXIMUM_TURN_RATE
                );
                output.add(new Pair<>(impulse, i * DIRECTION_INTERVAL));
            }
            return output;
        }

        public void tick(ScheduledTask task) {
            if (duration <= timestamp++ || currentImpulse == null) {
                task.cancel();
                if (player.isOnline() && player.getWorld() == startingPoint.getWorld()) {
                    Statistics.registerDrunkenTraversedBlocks(player.getLocation().distance(startingPoint));
                }
                return;
            }
            Vector walk = TheBrewingProject.getInstance().getPlayerWalkListener().getRegisteredMovement(player.getUniqueId());
            if (!player.isOnline() || walk == null || walk.lengthSquared() == 0D
                    || TheBrewingProject.getInstance().getActiveEventsRegistry().hasActiveEvent(player.getUniqueId(), NamedDrunkEvent.fromKey("stumble"))
            ) {
                return;
            }
            Pair<DrunkenImpulse, Integer> next = impulses.peek();
            if (next == null) {
                currentImpulse.applyTo(player, false);
                return;
            }
            DrunkenImpulse nextImpulse = next.first();
            int nextTimestamp = next.second();
            if (nextTimestamp <= timestamp) {
                pollNewCurrentImpulse(impulses);
                currentImpulse.applyTo(player, false);
                return;
            }
            double interpolation = (double) (nextTimestamp - timestamp) / (nextTimestamp - currentTimestamp);
            DrunkenImpulse newImpulse = DrunkenImpulse.lerp(nextImpulse, currentImpulse, interpolation);
            newImpulse.applyTo(player, false);
        }
    }

    @Override
    public int priority() {
        return -1;
    }

}
