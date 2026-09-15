package dev.jsinco.brewery.bukkit.database.barrel;

import com.google.gson.JsonParser;
import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.breweries.BarrelType;
import dev.jsinco.brewery.api.ingredient.ResolvedIngredientManager;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.util.BreweryRegistry;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.util.Pair;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.brew.BrewImpl;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.breweries.BrewInventoryImpl;
import dev.jsinco.brewery.bukkit.breweries.barrel.BukkitBarrel;
import dev.jsinco.brewery.bukkit.structure.BreweryStructure;
import dev.jsinco.brewery.bukkit.structure.PlacedBreweryStructure;
import dev.jsinco.brewery.database.PersistenceException;
import dev.jsinco.brewery.database.PersistenceSupplier;
import dev.jsinco.brewery.database.UncheckedPersistenceException;
import dev.jsinco.brewery.database.sql.SqlStatements;
import dev.jsinco.brewery.util.DecoderEncoder;
import dev.jsinco.brewery.util.FutureUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix3d;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public record SqLiteBarrelSession(Executor executor, PersistenceSupplier<Connection> connectionSupplier,
                                  CompletableFuture<ResolvedIngredientManager<ItemStack>> ingredientManagerFuture) implements BarrelSession {

    private static final SqlStatements BARREL_STATEMENTS = new SqlStatements("/database/generic/barrels");
    private static final SqlStatements BARREL_BREW_STATEMENTS = new SqlStatements("/database/generic/barrel_brews");


    @Override
    public CompletableFuture<Void> insertBrew(dev.jsinco.brewery.api.vector.Location barrelLocation, int inventoryPos, Brew brew) {
        return ingredientManagerFuture.thenAcceptAsync(ingredientManager -> {
            {
                try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BARREL_BREW_STATEMENTS.get(SqlStatements.Type.INSERT))) {
                    preparedStatement.setInt(1, barrelLocation.x());
                    preparedStatement.setInt(2, barrelLocation.y());
                    preparedStatement.setInt(3, barrelLocation.z());
                    preparedStatement.setBytes(4, DecoderEncoder.asBytes(barrelLocation.worldUuid()));
                    preparedStatement.setInt(5, inventoryPos);
                    preparedStatement.setString(6, BrewImpl.SERIALIZER.serialize(brew, ingredientManager).toString());
                    preparedStatement.execute();
                } catch (SQLException e) {
                    throw new UncheckedPersistenceException(e);
                }
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> removeBrew(dev.jsinco.brewery.api.vector.Location barrelLocation, int inventoryPos) {
        return execute(() -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BARREL_BREW_STATEMENTS.get(SqlStatements.Type.DELETE))) {
                preparedStatement.setInt(1, barrelLocation.x());
                preparedStatement.setInt(2, barrelLocation.y());
                preparedStatement.setInt(3, barrelLocation.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(barrelLocation.worldUuid()));
                preparedStatement.setInt(5, inventoryPos);
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<BrewLookupResult>> findBrews(dev.jsinco.brewery.api.vector.Location barrelLocation) {
        return ingredientManagerFuture.thenApplyAsync(ingredientManager -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BARREL_BREW_STATEMENTS.get(SqlStatements.Type.FIND))) {
                preparedStatement.setInt(1, barrelLocation.x());
                preparedStatement.setInt(2, barrelLocation.y());
                preparedStatement.setInt(3, barrelLocation.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(barrelLocation.worldUuid()));
                ResultSet resultSet = preparedStatement.executeQuery();
                List<BrewLookupResult> output = new ArrayList<>();
                while (resultSet.next()) {
                    final int pos = resultSet.getInt("pos");
                    output.add(new BrewLookupResult(brewFromResultSet(resultSet, ingredientManager), pos));
                }
                return output;
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
        }, executor);
    }

    private Brew brewFromResultSet(ResultSet resultSet, ResolvedIngredientManager<ItemStack> resolvedIngredientManager) throws SQLException {
        return BrewImpl.SERIALIZER.deserialize(JsonParser.parseString(resultSet.getString("brew")), resolvedIngredientManager);
    }

    @Override
    public CompletableFuture<Void> updateBrew(dev.jsinco.brewery.api.vector.Location barrelLocation, int inventoryPos, Brew newBrew) {
        return ingredientManagerFuture.thenAcceptAsync(ingredientManager -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BARREL_BREW_STATEMENTS.get(SqlStatements.Type.UPDATE))) {
                preparedStatement.setString(1, BrewImpl.SERIALIZER.serialize(newBrew, ingredientManager).toString());
                preparedStatement.setInt(2, barrelLocation.x());
                preparedStatement.setInt(3, barrelLocation.y());
                preparedStatement.setInt(4, barrelLocation.z());
                preparedStatement.setBytes(5, DecoderEncoder.asBytes(barrelLocation.worldUuid()));
                preparedStatement.setInt(6, inventoryPos);
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> insertBarrel(BukkitBarrel barrel) {
        PlacedBreweryStructure<BukkitBarrel> placedStructure = barrel.getStructure();
        BreweryStructure structure = placedStructure.getStructure();
        Location origin = placedStructure.getWorldOrigin();
        UUID worldUuid = barrel.getWorld().getUID();
        Location signLocation = barrel.getUniqueLocation();
        CompletableFuture<Void> completed = new CompletableFuture<>();
        execute(() -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BARREL_STATEMENTS.get(SqlStatements.Type.INSERT))) {
                preparedStatement.setInt(1, origin.getBlockX());
                preparedStatement.setInt(2, origin.getBlockY());
                preparedStatement.setInt(3, origin.getBlockZ());
                preparedStatement.setInt(4, signLocation.getBlockX());
                preparedStatement.setInt(5, signLocation.getBlockY());
                preparedStatement.setInt(6, signLocation.getBlockZ());
                preparedStatement.setBytes(7, DecoderEncoder.asBytes(worldUuid));
                preparedStatement.setString(8, DecoderEncoder.serializeTransformation(placedStructure.getTransformation()));
                preparedStatement.setString(9, structure.getName());
                preparedStatement.setString(10, barrel.getType().key().toString());
                preparedStatement.setInt(11, barrel.getSize());
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        }).thenRunAsync(() -> {
            List<CompletableFuture<Void>> completableFutures = new ArrayList<>();
            for (Pair<Brew, Integer> brew : barrel.getBrews()) {
                completableFutures.add(insertBrew(BukkitAdapter.toBreweryLocation(signLocation), brew.second(), brew.first()));
            }
            FutureUtil.mergeFutures(completableFutures).thenRun(() -> completed.complete(null));
        }, executor);
        return completed;
    }

    @Override
    public CompletableFuture<Void> removeBarrel(BukkitBarrel barrel) {
        return execute(() -> {
            UUID worldUuid = barrel.getWorld().getUID();
            Location signLocation = barrel.getUniqueLocation();
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BARREL_STATEMENTS.get(SqlStatements.Type.DELETE))) {
                preparedStatement.setInt(1, signLocation.getBlockX());
                preparedStatement.setInt(2, signLocation.getBlockY());
                preparedStatement.setInt(3, signLocation.getBlockZ());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(worldUuid));
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<BukkitBarrel>> findBarrels(UUID worldUuid) {
        CompletableFuture<List<BukkitBarrel>> completed = new CompletableFuture<>();
        fetch(() -> {
            List<BukkitBarrel> output = new ArrayList<>();
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BARREL_STATEMENTS.get(SqlStatements.Type.FIND))) {
                preparedStatement.setBytes(1, DecoderEncoder.asBytes(worldUuid));
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    Location worldOrigin = new Location(Bukkit.getWorld(worldUuid), resultSet.getInt("origin_x"), resultSet.getInt("origin_y"), resultSet.getInt("origin_z"));
                    Location uniqueLocation = new Location(Bukkit.getWorld(worldUuid), resultSet.getInt("unique_x"), resultSet.getInt("unique_y"), resultSet.getInt("unique_z"));
                    Matrix3d transform = DecoderEncoder.deserializeTransformation(resultSet.getString("transformation"));
                    String format = resultSet.getString("format");
                    BarrelType type = BreweryRegistry.BARREL_TYPE.get(BreweryKey.parse(resultSet.getString("barrel_type")));
                    if (type == null) {
                        Logger.logErr("Unknown barrel type '" + resultSet.getString("barrel_type") + "' for structure at: " + uniqueLocation);
                        continue;
                    }
                    int size = resultSet.getInt("size");

                    Optional<BreweryStructure> breweryStructureOptional = TheBrewingProject.getInstance().getStructureRegistry().getStructure(format);
                    if (breweryStructureOptional.isEmpty()) {
                        Logger.logErr("Could not find format '" + format + "' skipping barrel at: " + uniqueLocation);
                        continue;
                    }
                    PlacedBreweryStructure<BukkitBarrel> structure = new PlacedBreweryStructure<>(breweryStructureOptional.get(), transform, worldOrigin);
                    BukkitBarrel barrel = new BukkitBarrel(uniqueLocation, structure, size, type);
                    structure.setHolder(barrel);
                    output.add(barrel);
                }
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
            return output;
        }).thenAcceptAsync(barrels -> {
            List<CompletableFuture<Void>> barrelBrewsLoadedFuture = new ArrayList<>();
            for (BukkitBarrel barrel : barrels) {
                BrewInventoryImpl barrelInventory = barrel.getInventory();
                barrelBrewsLoadedFuture.add(findBrews(BukkitAdapter.toBreweryLocation(barrel.getUniqueLocation()))
                        .thenAccept(brews ->
                                brews.forEach(result -> barrelInventory.set(result.brew(), result.position()))
                        ));
            }
            FutureUtil.mergeFutures(barrelBrewsLoadedFuture)
                    .thenRun(() -> completed.complete(barrels));
        }, executor);
        return completed;
    }
}
