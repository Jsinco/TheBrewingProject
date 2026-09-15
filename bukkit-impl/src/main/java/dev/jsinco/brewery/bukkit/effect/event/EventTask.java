package dev.jsinco.brewery.bukkit.effect.event;

import dev.jsinco.brewery.api.event.EventPropertyExecutable;
import dev.jsinco.brewery.api.event.ExecutionOutcome;
import dev.jsinco.brewery.api.event.step.CustomEventCompleted;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.effect.step.CustomEventCompletedExecutable;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

final class EventTask implements Consumer<ScheduledTask> {
    private final LinkedList<EventPropertyExecutable> executables;
    private final DrunkEventExecutor executor;
    private final UUID playerUuid;
    private final Object playerTaskLock = new Object();
    private @Nullable ScheduledTask playerTask = null;
    private long waitTime = 0L;
    private final Object waitTimeLock = new Object();

    EventTask(LinkedList<EventPropertyExecutable> executables,
              UUID playerUuid, DrunkEventExecutor executor) {
        this.executables = executables;
        this.playerUuid = playerUuid;
        this.executor = executor;
    }

    @Override
    public void accept(ScheduledTask scheduledTask) {
        if (executables.isEmpty()) {
            scheduledTask.cancel();
            return;
        }
        synchronized (waitTimeLock) {
            if (waitTime > 0) {
                --waitTime;
                return;
            }
        }
        synchronized (playerTaskLock) {
            if (playerTask != null && !playerTask.isCancelled()) {
                return;
            }
        }
        EventPropertyExecutable first = executables.getFirst();
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            if (!FeaturesConfig.test(FeatureFlag.BREW_EFFECTS, player.getWorld().getName())) {
                scheduledTask.cancel();
                return;
            }
            synchronized (playerTaskLock) {
                this.playerTask = player.getScheduler().runAtFixedRate(
                        TheBrewingProject.getInstance(),
                        executor(EventPropertyExecutable.ExecutionContext.PLAYER),
                        () -> {
                        },
                        1L,
                        1L
                );
            }
            return;
        }
        if (first.context() == EventPropertyExecutable.ExecutionContext.PLAYER) {
            while (!executables.isEmpty() && executables.getFirst().context() == EventPropertyExecutable.ExecutionContext.PLAYER) {
                if (executables.removeFirst() instanceof CustomEventCompletedExecutable(
                        CustomEventCompleted eventCompleted
                )) {
                    executor.unregisterEvent(playerUuid, eventCompleted.eventKey());
                }
            }
            return;
        }
        executor(EventPropertyExecutable.ExecutionContext.ANY).accept(scheduledTask);
    }


    private Consumer<ScheduledTask> executor(EventPropertyExecutable.ExecutionContext context) {
        return task -> {
            EventPropertyExecutable skipTarget = null;
            while (!executables.isEmpty()) {
                EventPropertyExecutable executable = executables.getFirst();
                if (executable instanceof CustomEventCompletedExecutable(
                        CustomEventCompleted eventCompleted
                )) {
                    executor.unregisterEvent(playerUuid, eventCompleted.eventKey());
                }
                if (skipTarget != null) {
                    if (executable == skipTarget) {
                        skipTarget = null;
                    }
                    executables.removeFirst();
                    continue;
                }
                if (executable.context() == EventPropertyExecutable.ExecutionContext.PLAYER && context != EventPropertyExecutable.ExecutionContext.PLAYER) {
                    return;
                }
                executables.removeFirst();
                ExecutionOutcome outcome = executable.executeFor(playerUuid);
                switch (outcome) {
                    case ExecutionOutcome.Continue ignored -> {
                        // NO-OP
                    }
                    case ExecutionOutcome.InsertSteps insertSteps -> {
                        executables.addAll(0, insertSteps.executables());
                    }
                    case ExecutionOutcome.Skip skip -> {
                        skipTarget = skip.skipPast();
                    }
                    case ExecutionOutcome.SkipAll ignored -> {
                        executor.unregisterEvents(playerUuid, executables);
                        executables.clear();
                        task.cancel();
                        return;
                    }
                    case ExecutionOutcome.Wait wait -> {
                        synchronized (waitTimeLock) {
                            waitTime = wait.ticks();
                        }
                        if (EventPropertyExecutable.ExecutionContext.PLAYER == context) {
                            task.cancel();
                        }
                        return;
                    }
                    case ExecutionOutcome.WaitCondition waitCondition -> {
                        waitCondition.executablesConsumer().accept(List.copyOf(executables));
                        executables.clear();
                        task.cancel();
                        return;
                    }
                }
            }
            task.cancel();
        };
    }
}
