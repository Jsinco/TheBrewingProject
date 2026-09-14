package dev.jsinco.brewery.bukkit.structure;

import dev.jsinco.brewery.api.breweries.StructureHolder;
import dev.jsinco.brewery.api.structure.MultiblockStructure;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import org.bukkit.Location;
import org.joml.Matrix3d;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PlacedBreweryStructure<H extends StructureHolder<H>> implements MultiblockStructure<H> {
    private static final List<Matrix3d> ALLOWED_TRANSFORMATIONS = compileAllowedTransformations();
    private final BreweryStructure structure;
    private final Matrix3d transformation;
    private final Location worldOrigin;
    private final dev.jsinco.brewery.api.vector.Location unique;
    private @Nullable H holder = null;

    public PlacedBreweryStructure(BreweryStructure structure, Matrix3d transformation,
                                  Location worldOrigin) {
        this.structure = structure;
        this.transformation = transformation;
        this.worldOrigin = worldOrigin;
        this.unique = compileUnique();
    }

    public static <H extends StructureHolder<H>> Optional<Pair<PlacedBreweryStructure<H>, BreweryKey>> findValid(BreweryStructure structure, Location worldOrigin) {
        for (Matrix3d transformation : ALLOWED_TRANSFORMATIONS) {
            for (StructureMatcher matcher : structure.getStructureMatchers()) {
                Optional<Location> possibleOrigin = structure.findValidOrigin(transformation, worldOrigin, matcher);
                if (possibleOrigin.isPresent()) {
                    return possibleOrigin
                            .map(origin -> new Pair<>(new PlacedBreweryStructure<>(structure, transformation, origin), matcher.typeKey()));
                }
            }
        }
        return Optional.empty();
    }

    public List<dev.jsinco.brewery.api.vector.Location> positions() {
        return structure.getExpectedBlocks(transformation, worldOrigin)
                .keySet()
                .stream()
                .map(location -> new BreweryLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getUID()))
                .map(dev.jsinco.brewery.api.vector.Location.class::cast)
                .toList();
    }

    @Override
    public dev.jsinco.brewery.api.vector.Location getUnique() {
        return unique;
    }

    private dev.jsinco.brewery.api.vector.Location compileUnique() {
        List<dev.jsinco.brewery.api.vector.Location> positions = new ArrayList<>(positions());
        positions.sort(this::comparePositions);
        return positions.getFirst();
    }

    private static List<Matrix3d> compileAllowedTransformations() {
        List<Matrix3d> output = new ArrayList<>();
        Matrix3d transformation = new Matrix3d();
        for (int i = 0; i < 4; i++) {
            output.add(roundMatrix(transformation.rotate(Math.PI / 2 * i, 0, 1, 0, new Matrix3d())));
        }
        transformation.reflect(1, 0, 0);
        for (int i = 0; i < 4; i++) {
            output.add(roundMatrix(transformation.rotate(Math.PI / 2 * i, 0, 1, 0, new Matrix3d())));
        }
        return List.copyOf(output);
    }

    private static Matrix3d roundMatrix(Matrix3d matrix3d) {
        double[] doubles = matrix3d.get(new double[9]);
        matrix3d.set(Arrays.stream(doubles)
                .map(Math::round)
                .toArray()
        );
        return matrix3d;
    }

    private int comparePositions(dev.jsinco.brewery.api.vector.Location breweryLocation, dev.jsinco.brewery.api.vector.Location breweryLocation1) {
        if (breweryLocation.y() > breweryLocation1.y()) {
            return -1;
        }
        if (breweryLocation.x() > breweryLocation1.x()) {
            return -1;
        }
        if (breweryLocation.z() > breweryLocation1.z()) {
            return -1;
        }
        return 0;
    }

    public BreweryStructure getStructure() {
        return this.structure;
    }

    public Matrix3d getTransformation() {
        return this.transformation;
    }

    public Location getWorldOrigin() {
        return this.worldOrigin;
    }

    @Nullable
    public H getHolder() {
        return this.holder;
    }

    public void setHolder(@Nullable H holder) {
        this.holder = holder;
    }
}
