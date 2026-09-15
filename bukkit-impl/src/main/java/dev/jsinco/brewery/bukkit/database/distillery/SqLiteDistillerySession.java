package dev.jsinco.brewery.bukkit.database.distillery;

import com.google.gson.JsonParser;
import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.ingredient.ResolvedIngredientManager;
import dev.jsinco.brewery.api.util.Logger;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.brew.BrewImpl;
import dev.jsinco.brewery.bukkit.TheBrewingProject;
import dev.jsinco.brewery.bukkit.api.BukkitAdapter;
import dev.jsinco.brewery.bukkit.breweries.BrewInventoryImpl;
import dev.jsinco.brewery.bukkit.breweries.distillery.BukkitDistillery;
import dev.jsinco.brewery.bukkit.structure.BreweryStructure;
import dev.jsinco.brewery.bukkit.structure.PlacedBreweryStructure;
import dev.jsinco.brewery.database.PersistenceException;
import dev.jsinco.brewery.database.PersistenceSupplier;
import dev.jsinco.brewery.database.UncheckedPersistenceException;
import dev.jsinco.brewery.database.sql.SqlStatements;
import dev.jsinco.brewery.util.DecoderEncoder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

public record SqLiteDistillerySession(Executor executor, PersistenceSupplier<Connection> connectionSupplier,
                                      CompletableFuture<ResolvedIngredientManager<ItemStack>> ingredientManagerFuture) implements DistillerySession {
    private static final SqlStatements BREW_DISTILLERY_STATEMENTS = new SqlStatements("/database/generic/distillery_brews");
    private static final SqlStatements DISTILLERY_STATEMENTS = new SqlStatements("/database/generic/distilleries");

    @Override
    public CompletableFuture<Void> insertBrew(dev.jsinco.brewery.api.vector.Location distilleryLocation, int inventoryPos, boolean distillateInventoryType, Brew brew) {
        return ingredientManagerFuture.thenAcceptAsync(ingredientManager -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BREW_DISTILLERY_STATEMENTS.get(SqlStatements.Type.INSERT))) {
                preparedStatement.setInt(1, distilleryLocation.x());
                preparedStatement.setInt(2, distilleryLocation.y());
                preparedStatement.setInt(3, distilleryLocation.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(distilleryLocation.worldUuid()));
                preparedStatement.setInt(5, inventoryPos);
                preparedStatement.setBoolean(6, distillateInventoryType);
                preparedStatement.setString(7, BrewImpl.SERIALIZER.serialize(brew, ingredientManager).toString());
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> removeBrew(dev.jsinco.brewery.api.vector.Location distilleryLocation, int inventoryPos, boolean distillateInventoryType) {
        return execute(() -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BREW_DISTILLERY_STATEMENTS.get(SqlStatements.Type.DELETE))) {
                preparedStatement.setInt(1, distilleryLocation.x());
                preparedStatement.setInt(2, distilleryLocation.y());
                preparedStatement.setInt(3, distilleryLocation.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(distilleryLocation.worldUuid()));
                preparedStatement.setInt(5, inventoryPos);
                preparedStatement.setBoolean(6, distillateInventoryType);
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<BrewLookupResult>> findBrews(dev.jsinco.brewery.api.vector.Location distilleryLocation) {
        return ingredientManagerFuture.thenApplyAsync(ingredientManager -> {
            List<BrewLookupResult> output = new ArrayList<>();
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BREW_DISTILLERY_STATEMENTS.get(SqlStatements.Type.FIND))) {
                preparedStatement.setInt(1, distilleryLocation.x());
                preparedStatement.setInt(2, distilleryLocation.y());
                preparedStatement.setInt(3, distilleryLocation.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(distilleryLocation.worldUuid()));
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    int pos = resultSet.getInt("pos");
                    boolean isDistillate = resultSet.getBoolean("is_distillate");
                    Brew brew = BrewImpl.SERIALIZER.deserialize(JsonParser.parseString(resultSet.getString("brew")), ingredientManager);
                    output.add(new BrewLookupResult(brew, pos, isDistillate));
                }
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
            return output;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updateBrew(dev.jsinco.brewery.api.vector.Location distilleryLocation, int inventoryPos, boolean distillateInventoryType, Brew newBrew) {
        return ingredientManagerFuture.thenAcceptAsync(ingredientManager -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(BREW_DISTILLERY_STATEMENTS.get(SqlStatements.Type.UPDATE))) {
                preparedStatement.setString(1, BrewImpl.SERIALIZER.serialize(newBrew, ingredientManager).toString());
                preparedStatement.setInt(2, distilleryLocation.x());
                preparedStatement.setInt(3, distilleryLocation.y());
                preparedStatement.setInt(4, distilleryLocation.z());
                preparedStatement.setBytes(5, DecoderEncoder.asBytes(distilleryLocation.worldUuid()));
                preparedStatement.setInt(6, inventoryPos);
                preparedStatement.setBoolean(7, distillateInventoryType);
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> insertDistillery(BukkitDistillery distillery) {
        return execute(() -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(DISTILLERY_STATEMENTS.get(SqlStatements.Type.INSERT))) {
                PlacedBreweryStructure<BukkitDistillery> structure = distillery.getStructure();
                BreweryLocation origin = BukkitAdapter.toBreweryLocation(structure.getWorldOrigin());
                dev.jsinco.brewery.api.vector.Location unique = structure.getUnique();
                preparedStatement.setInt(1, origin.x());
                preparedStatement.setInt(2, origin.y());
                preparedStatement.setInt(3, origin.z());
                preparedStatement.setInt(4, unique.x());
                preparedStatement.setInt(5, unique.y());
                preparedStatement.setInt(6, unique.z());
                preparedStatement.setBytes(7, DecoderEncoder.asBytes(origin.worldUuid()));
                preparedStatement.setString(8, DecoderEncoder.serializeTransformation(structure.getTransformation()));
                preparedStatement.setString(9, structure.getStructure().getName());
                preparedStatement.setLong(10, distillery.getStartTime());
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeDistillery(BukkitDistillery distillery) {
        return execute(() -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(DISTILLERY_STATEMENTS.get(SqlStatements.Type.DELETE))) {
                dev.jsinco.brewery.api.vector.Location unique = distillery.getStructure().getUnique();
                preparedStatement.setInt(1, unique.x());
                preparedStatement.setInt(2, unique.y());
                preparedStatement.setInt(3, unique.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(unique.worldUuid()));
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<BukkitDistillery>> findDistilleries(UUID worldUuid) {
        CompletableFuture<List<BukkitDistillery>> distilleriesFuture = new CompletableFuture<>();
        fetch(() -> {
            List<BukkitDistillery> output = new ArrayList<>();
            World world = Bukkit.getWorld(worldUuid);
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(DISTILLERY_STATEMENTS.get(SqlStatements.Type.FIND))) {
                preparedStatement.setBytes(1, DecoderEncoder.asBytes(worldUuid));
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    int originX = resultSet.getInt("origin_x");
                    int originY = resultSet.getInt("origin_y");
                    int originZ = resultSet.getInt("origin_z");
                    Location structureOrigin = new Location(world, originX, originY, originZ);
                    String structureName = resultSet.getString("format");
                    Optional<BreweryStructure> breweryStructure = TheBrewingProject.getInstance().getStructureRegistry().getStructure(structureName);
                    if (breweryStructure.isEmpty()) {
                        Logger.logErr("Could not find format '" + structureName + "' skipping distillery at: " + structureOrigin);
                        continue;
                    }
                    Matrix3d transformation = DecoderEncoder.deserializeTransformation(resultSet.getString("transformation"));
                    PlacedBreweryStructure<BukkitDistillery> placedBreweryStructure = new PlacedBreweryStructure<>(breweryStructure.get(), transformation, structureOrigin);
                    int startTime = resultSet.getInt("start_time");
                    BukkitDistillery bukkitDistillery = new BukkitDistillery(placedBreweryStructure, startTime);
                    placedBreweryStructure.setHolder(bukkitDistillery);
                    output.add(bukkitDistillery);
                }
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
            return output;
        }).thenAccept(distilleries ->
                insertBrews(distilleries).thenRun(() -> distilleriesFuture.complete(distilleries))
        );
        return distilleriesFuture;
    }

    private CompletableFuture<Void> insertBrews(List<BukkitDistillery> distilleries) {
        List<CompletableFuture<Void>> brewsInsertedFutures = new ArrayList<>();
        for (BukkitDistillery distillery : distilleries) {
            brewsInsertedFutures.add(findBrews(distillery.getStructure().getUnique())
                    .thenAccept(brews -> brews.forEach(brew -> {
                        BrewInventoryImpl inventory = brew.distillateInventoryType()
                                ? distillery.getDistillate() : distillery.getMixture();
                        inventory.set(brew.brew(), brew.position());
                    })));
        }
        return CompletableFuture.allOf(brewsInsertedFutures.toArray(CompletableFuture<?>[]::new));
    }

    @Override
    public CompletableFuture<Void> updateDistillery(BukkitDistillery newDistillery) {
        return execute(() -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(DISTILLERY_STATEMENTS.get(SqlStatements.Type.UPDATE))) {
                long startTime = newDistillery.getStartTime();
                dev.jsinco.brewery.api.vector.Location unique = newDistillery.getStructure().getUnique();
                preparedStatement.setLong(1, startTime);
                preparedStatement.setInt(2, unique.x());
                preparedStatement.setInt(3, unique.y());
                preparedStatement.setInt(4, unique.z());
                preparedStatement.setBytes(5, DecoderEncoder.asBytes(unique.worldUuid()));
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }
}
