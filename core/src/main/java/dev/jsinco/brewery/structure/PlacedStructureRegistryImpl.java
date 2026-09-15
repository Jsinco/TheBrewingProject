package dev.jsinco.brewery.structure;

import dev.jsinco.brewery.api.breweries.StructureHolder;
import dev.jsinco.brewery.api.structure.MultiblockStructure;
import dev.jsinco.brewery.api.structure.PlacedStructureRegistry;
import dev.jsinco.brewery.api.structure.StructureType;
import dev.jsinco.brewery.api.vector.Location;
import dev.jsinco.brewery.api.vector.Vector;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlacedStructureRegistryImpl implements PlacedStructureRegistry {

    private final Map<UUID, Map<Vector, MultiblockStructure<? extends StructureHolder<?>>>> structures = new HashMap<>();
    private final Map<StructureType<?>, Set<MultiblockStructure<?>>> typedMultiBlockStructureMap = new HashMap<>();

    public synchronized void registerStructures(Collection<? extends MultiblockStructure<?>> multiblockStructures) {
        multiblockStructures.forEach(this::registerStructure);
    }

    @Override
    public synchronized void registerStructure(MultiblockStructure<?> multiblockStructure) {
        for (Location location : multiblockStructure.positions()) {
            UUID worldUuid = location.worldUuid();
            structures.computeIfAbsent(worldUuid, ignored -> new HashMap<>()).put(location.toVector(), multiblockStructure);
        }
        typedMultiBlockStructureMap.computeIfAbsent(multiblockStructure.getHolder().getStructureType(), ignored -> new HashSet<>()).add(multiblockStructure);
    }

    @Override
    public synchronized void unregisterStructure(MultiblockStructure<?> structure) {
        for (Location location : structure.positions()) {
            UUID worldUuid = location.worldUuid();
            structures.computeIfAbsent(worldUuid, ignored -> new HashMap<>()).remove(location.toVector());
        }
        typedMultiBlockStructureMap.computeIfAbsent(structure.getHolder().getStructureType(), ignored -> new HashSet<>()).remove(structure);
    }

    @Override
    public Optional<MultiblockStructure<?>> getStructure(Location location) {
        UUID worldUuid = location.worldUuid();
        Map<Vector, MultiblockStructure<?>> placedBreweryStructureMap = structures.getOrDefault(worldUuid, Map.of());
        return Optional.ofNullable(placedBreweryStructureMap.get(location.toVector()));
    }

    @Override
    public Set<MultiblockStructure<?>> getStructures(Collection<Location> locations) {
        Set<MultiblockStructure<?>> breweryStructures = new HashSet<>();
        for (Location location : locations) {
            getStructure(location).ifPresent(breweryStructures::add);
        }
        return breweryStructures;
    }

    @Override
    public synchronized int countStructureType(StructureType<?> structureType) {
        if (!typedMultiBlockStructureMap.containsKey(structureType)) {
            return 0;
        }
        return typedMultiBlockStructureMap.get(structureType).size();
    }

    public Set<MultiblockStructure<?>> getStructures(StructureType<?> structureType) {
        return typedMultiBlockStructureMap.computeIfAbsent(structureType, ignored -> new HashSet<>());
    }

    @Override
    public Optional<StructureHolder<?>> getHolder(Location location) {
        UUID worldUuid = location.worldUuid();
        Map<Vector, MultiblockStructure<? extends StructureHolder<?>>> placedBreweryStructureMap = structures.getOrDefault(worldUuid, new HashMap<>());
        return Optional.ofNullable(placedBreweryStructureMap.get(location.toVector()))
                .map(MultiblockStructure::getHolder);
    }

    @Override
    public void unloadWorld(UUID worldUuid) {
        Map<Vector, MultiblockStructure<? extends StructureHolder<?>>> removed = structures.remove(worldUuid);
        if (removed == null) {
            return;
        }
        removed.forEach((ignored1, structure) -> {
            typedMultiBlockStructureMap.computeIfAbsent(structure.getHolder().getStructureType(), ignored2 -> new HashSet<>()).remove(structure);
        });
    }

    @Override
    public void clear() {
        structures.clear();
    }
}
