package dev.jsinco.brewery.effect;

import dev.jsinco.brewery.api.effect.DrunkState;
import dev.jsinco.brewery.api.effect.DrunksManager;
import dev.jsinco.brewery.api.effect.ModifierConsume;
import dev.jsinco.brewery.api.effect.modifier.DrunkenModifier;
import dev.jsinco.brewery.api.event.CustomEventRegistry;
import dev.jsinco.brewery.api.event.DrunkEvent;
import dev.jsinco.brewery.api.event.EventData;
import dev.jsinco.brewery.api.moment.Moment;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.configuration.DrunkenModifierSection;
import dev.jsinco.brewery.configuration.EventSection;
import dev.jsinco.brewery.configuration.features.FeatureFlag;
import dev.jsinco.brewery.configuration.features.FeaturesConfig;
import dev.jsinco.brewery.database.PersistenceException;
import dev.jsinco.brewery.database.PersistenceSupplier;
import dev.jsinco.brewery.database.session.DrunkenStateSession;
import dev.jsinco.brewery.util.RandomUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrunksManagerImpl<C> implements DrunksManager {

    private final CustomEventRegistry eventRegistry;
    private final PersistenceSupplier<DrunkenStateSession> sessionSupplier;
    private final Function<EventData, Optional<DrunkEvent>> eventSupplier;
    private Set<EventData> allowedEvents;
    private Map<UUID, DrunkState> drunks = new ConcurrentHashMap<>();
    private LongSupplier timeSupplier;
    private Map<Long, Map<UUID, DrunkEvent>> events = new ConcurrentHashMap<>();
    private Map<UUID, Long> plannedEvents = new ConcurrentHashMap<>();
    private final Function<UUID, String> playerUuidToWorldName;

    private static final Random RANDOM = new Random();

    public DrunksManagerImpl(CustomEventRegistry registry, Set<EventData> allowedEvents,
                             Function<EventData, Optional<DrunkEvent>> eventSupplier, LongSupplier timeSupplier,
                             PersistenceSupplier<DrunkenStateSession> sessionSupplier,
                             Function<UUID, String> playerUuidToWorldName) {
        this.eventRegistry = registry;
        this.allowedEvents = allowedEvents;
        this.timeSupplier = timeSupplier;
        this.eventSupplier = eventSupplier;
        this.sessionSupplier = sessionSupplier;
        this.playerUuidToWorldName = playerUuidToWorldName;
        loadDrunkStates();
    }

    private void loadDrunkStates() {
        try {
            DrunkenStateSession session = sessionSupplier.get();
            session.retrieveAllStates()
                    .thenAccept(states ->
                            states.forEach(state -> drunks.put(state.playerUuid(), state.state()))
                    ).exceptionally(Logger::logAndTrackErr);
        } catch (PersistenceException e) {
            Logger.logErr(e);
        }
    }

    @Override
    public @Nullable DrunkState consume(UUID playerUuid, String modifierName, double value) {
        return consume(playerUuid,
                new ModifierConsume(
                        DrunkenModifierSection.modifiers().modifier(modifierName),
                        value
                ));
    }

    @Override
    public @Nullable DrunkState consume(UUID playerUuid, List<ModifierConsume> consumptions) {
        return this.consume(playerUuid, consumptions, timeSupplier.getAsLong());
    }

    @Override
    public @Nullable DrunkState consume(UUID playerUuid, ModifierConsume modifier) {
        return this.consume(playerUuid, List.of(modifier));
    }

    /**
     * @param timestamp Should be in relation to the internal clock in drunk manager
     */
    public @Nullable DrunkState consume(UUID playerUuid, List<ModifierConsume> modifiers, long timestamp) {
        boolean alreadyDrunk = drunks.containsKey(playerUuid);
        DrunkState initialState = (alreadyDrunk ? drunks.get(playerUuid).recalculate(timestamp) : new DrunkStateImpl(
                timestamp, -1, DrunkenModifierSection.modifiers()
                               .drunkenModifiers().stream()
                               .collect(Collectors.toUnmodifiableMap(temp -> temp, DrunkenModifier::minValue))
        ));
        DrunkState newState = initialState;
        // Behave exactly the same when a modifier is changing
        List<ModifierConsume> sortedModifiers = new ArrayList<>(modifiers);
        sortedModifiers.sort(
                Comparator.comparing(modifierConsume -> modifierConsume.modifier().name(), String::compareTo)
        );
        for (ModifierConsume modifierConsume : sortedModifiers) {
            if (modifierConsume.cascade()) {
                Pair<DrunkState, Boolean> drunkStateChange = newState.cascadeModifier(modifierConsume.modifier(), modifierConsume.value());
                newState = drunkStateChange.first();
                if (drunkStateChange.second()) {
                    continue;
                }
            }
            newState = newState.setModifier(modifierConsume.modifier(), modifierConsume.value() + newState.modifierValue(modifierConsume.modifier()));
        }
        if (newState.additionalModifierData().isEmpty() && !isPassedOut(newState)) {
            drunks.remove(playerUuid);
            if (alreadyDrunk) {
                try {
                    sessionSupplier.get().removeState(playerUuid)
                            .exceptionally(Logger::logAndTrackErr);
                } catch (PersistenceException e) {
                    Logger.logErr(e);
                }
            }
            return null;
        }
        drunks.put(playerUuid, newState);
        planEvent(playerUuid);
        updateState(playerUuid, alreadyDrunk, newState, initialState);
        return newState;
    }

    @Override
    public @Nullable DrunkState getDrunkState(UUID playerUuid) {
        if (!drunks.containsKey(playerUuid)) {
            return null;
        }
        DrunkState drunkState = drunks.get(playerUuid);
        long previousTimestamp = drunkState.timestamp();
        DrunkState recalculated;
        if (!FeaturesConfig.test(FeatureFlag.MODIFIER_CHANGE, playerUuidToWorldName.apply(playerUuid))) {
            recalculated = moveTimestampToNow(drunkState);
        } else {
            recalculated = drunkState.recalculate(timeSupplier.getAsLong());
        }
        if (recalculated.additionalModifierData().isEmpty()) {
            clear(playerUuid);
            return null;
        }
        if (previousTimestamp + 20 * Moment.SECOND < recalculated.timestamp()) {
            drunks.put(playerUuid, recalculated);
            updateState(playerUuid, true, recalculated, drunkState);
        }
        return recalculated;
    }

    private void updateState(UUID playerUuid, boolean alreadyDrunk, DrunkState newState, DrunkState oldState) {
        DrunkenStateSession session;
        try {
            session = sessionSupplier.get();
        } catch (PersistenceException e) {
            Logger.logErr(e);
            return;
        }
        CompletableFuture<Void> future;
        if (alreadyDrunk) {
            future = session.updateState(newState, playerUuid);
        } else {
            future = session.insertState(newState, playerUuid);
        }
        Set<DrunkenModifier> allModifiers = Stream.concat(oldState.additionalModifierData().stream(), newState.additionalModifierData().stream())
                .map(Pair::first)
                .collect(Collectors.toSet());
        Map<DrunkenModifier, Double> newModifiers = newState.modifiers();
        future.thenAcceptAsync(ignored -> {
            for (DrunkenModifier modifier : allModifiers) {
                if (newModifiers.get(modifier) != modifier.minValue()) {
                    session.insertModifier(modifier, newModifiers.get(modifier), playerUuid)
                            .exceptionally(Logger::logAndTrackErr);
                }
                if (newModifiers.get(modifier) == modifier.minValue()) {
                    session.removeModifier(modifier, playerUuid)
                            .exceptionally(Logger::logAndTrackErr);
                }
            }
        }).exceptionally(Logger::logAndTrackErr);
    }

    @Override
    public void reset(@NonNull Set<EventData> allowedEvents) {
        plannedEvents.clear();
        drunks.clear();
        this.allowedEvents = allowedEvents;
        events.clear();
        loadDrunkStates();
        drunks.keySet().forEach(this::planEvent);
    }

    @Override
    public void clear(@NonNull UUID playerUuid) {
        Long plannedEventTime = plannedEvents.remove(playerUuid);
        drunks.remove(playerUuid);
        try {
            sessionSupplier.get().removeState(playerUuid)
                    .exceptionally(Logger::logAndTrackErr);
        } catch (PersistenceException e) {
            Logger.logErr(e);
        }
        if (plannedEventTime == null) {
            return;
        }
        if (events.containsKey(plannedEventTime)) {
            events.get(plannedEventTime).remove(playerUuid);
        }
    }

    public void tick(BiConsumer<UUID, DrunkEvent> action, Predicate<UUID> onlinePredicate) {
        Map<UUID, DrunkEvent> currentEvents = events.remove(timeSupplier.getAsLong());
        if (currentEvents == null) {
            return;
        }
        Set<UUID> toRemove = new HashSet<>();
        for (UUID currentEvent : currentEvents.keySet()) {
            if (!drunks.containsKey(currentEvent)) {
                toRemove.add(currentEvent);
            }
        }
        currentEvents.forEach((key, value) -> plannedEvents.remove(key));
        toRemove.forEach(currentEvents::remove);
        currentEvents.forEach(action);
        currentEvents.keySet()
                .stream()
                .filter(onlinePredicate)
                .forEach(this::planEvent);
    }

    @Override
    public void planEvent(@NonNull UUID playerUuid) {
        DrunkState drunkState = getDrunkState(playerUuid);
        if (drunkState == null) {
            return;
        }
        List<Pair<DrunkEvent, Double>> drunkEvents = allowedEvents.stream()
                .map(eventSupplier)
                .flatMap(Optional::stream)
                .map(drunkEvent -> new Pair<>(drunkEvent, drunkEvent.probability().evaluate(DrunkStateImpl.compileVariables(drunkState.modifiers(), null, 0D))))
                .filter(drunkEvent -> drunkEvent.second().enabled())
                .map(drunkEvent -> new Pair<>(drunkEvent.first(), drunkEvent.second().probability()))
                .filter(drunkEvent -> drunkEvent.second() > 0)
                .toList();
        if (drunkEvents.isEmpty()) {
            return;
        }
        double cumulativeSum = drunkEvents.stream()
                .map(Pair::second)
                .reduce(0D, Double::sum);
        DrunkEvent drunkEvent = RandomUtil.randomWeighted(drunkEvents, Pair::second).first();
        double value = (double) 20 / cumulativeSum;
        long time = (long) (timeSupplier.getAsLong() + Math.max(1, RANDOM.nextGaussian(value, value / 2)));
        if (plannedEvents.containsKey(playerUuid)) {
            if (plannedEvents.get(playerUuid) < time) {
                return;
            }
            events.get(plannedEvents.get(playerUuid)).remove(playerUuid);
        }
        events.computeIfAbsent(time, ignored -> new HashMap<>()).put(playerUuid, drunkEvent);
        plannedEvents.put(playerUuid, time);
    }

    @Override
    public void registerPassedOut(@NonNull UUID playerUuid) {
        drunks.computeIfPresent(playerUuid, (ignored, drunkState) -> drunkState.withPassOut(timeSupplier.getAsLong()));
    }

    @Override
    public boolean isPassedOut(@NonNull UUID playerUUID) {
        return drunks.containsKey(playerUUID) && isPassedOut(drunks.get(playerUUID));
    }

    private boolean isPassedOut(DrunkState drunkState) {
        long passOutTimeStamp = drunkState.kickedTimestamp();
        if (passOutTimeStamp == -1) {
            return false;
        }
        return passOutTimeStamp + EventSection.events().passOutTime().durationTicks() > timeSupplier.getAsLong();
    }

    @Override
    public @Nullable Pair<DrunkEvent, Long> getPlannedEvent(@NonNull UUID playerUUID) {
        Long time = plannedEvents.get(playerUUID);
        if (time == null) {
            return null;
        }
        return new Pair<>(events.get(time).get(playerUUID), time);
    }

    public void freezeDrunkState(UUID playerUuid) {
        DrunkState drunkState = drunks.get(playerUuid);
        if (drunkState == null) {
            return;
        }
        updateState(
                playerUuid,
                true,
                moveTimestampToNow(drunkState),
                drunkState
        );
    }

    private DrunkState moveTimestampToNow(DrunkState drunkState) {
        return new DrunkStateImpl(timeSupplier.getAsLong(), drunkState.kickedTimestamp(), drunkState.modifiers());
    }
}
