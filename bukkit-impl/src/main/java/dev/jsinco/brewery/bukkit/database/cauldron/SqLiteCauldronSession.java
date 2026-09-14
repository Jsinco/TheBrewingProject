package dev.jsinco.brewery.bukkit.database.cauldron;

import com.google.gson.JsonParser;
import dev.jsinco.brewery.api.brew.Brew;
import dev.jsinco.brewery.api.brew.BrewingStep;
import dev.jsinco.brewery.api.breweries.CauldronType;
import dev.jsinco.brewery.api.ingredient.ResolvedIngredientManager;
import dev.jsinco.brewery.api.util.BreweryKey;
import dev.jsinco.brewery.api.vector.BreweryLocation;
import dev.jsinco.brewery.api.vector.Location;
import dev.jsinco.brewery.brew.BrewImpl;
import dev.jsinco.brewery.bukkit.breweries.BukkitCauldron;
import dev.jsinco.brewery.database.PersistenceException;
import dev.jsinco.brewery.database.PersistenceSupplier;
import dev.jsinco.brewery.database.UncheckedPersistenceException;
import dev.jsinco.brewery.database.sql.SqlStatements;
import dev.jsinco.brewery.util.DecoderEncoder;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public record SqLiteCauldronSession(Executor executor, PersistenceSupplier<Connection> connectionSupplier,
                                    CompletableFuture<ResolvedIngredientManager<ItemStack>> ingredientManagerFuture) implements CauldronSession {
    private static final SqlStatements STATEMENTS = new SqlStatements("/database/generic/cauldrons");

    @Override
    public CompletableFuture<Void> insertCauldron(BukkitCauldron cauldron) {
        return ingredientManagerFuture.thenAcceptAsync(ingredientManager -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(STATEMENTS.get(SqlStatements.Type.INSERT))) {
                Location location = cauldron.position();
                preparedStatement.setInt(1, location.x());
                preparedStatement.setInt(2, location.y());
                preparedStatement.setInt(3, location.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(location.worldUuid()));
                preparedStatement.setString(5, BrewImpl.SERIALIZER.serialize(cauldron.getBrew(), ingredientManager).toString());
                preparedStatement.setString(6, cauldron.getCauldronType().key().minimalized());
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> updateCauldron(BukkitCauldron newCauldron) {
        return ingredientManagerFuture.thenAcceptAsync(ingredientManager -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(STATEMENTS.get(SqlStatements.Type.UPDATE))) {
                Location location = newCauldron.position();
                preparedStatement.setString(1, BrewImpl.SERIALIZER.serialize(newCauldron.getBrew(), ingredientManager).toString());
                preparedStatement.setString(2, newCauldron.getCauldronType().key().minimalized());
                preparedStatement.setInt(3, location.x());
                preparedStatement.setInt(4, location.y());
                preparedStatement.setInt(5, location.z());
                preparedStatement.setBytes(6, DecoderEncoder.asBytes(location.worldUuid()));
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> removeCauldron(BukkitCauldron cauldron) {
        return execute(() -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(STATEMENTS.get(SqlStatements.Type.DELETE))) {
                Location location = cauldron.position();
                preparedStatement.setInt(1, location.x());
                preparedStatement.setInt(2, location.y());
                preparedStatement.setInt(3, location.z());
                preparedStatement.setBytes(4, DecoderEncoder.asBytes(location.worldUuid()));
                preparedStatement.execute();
            } catch (SQLException e) {
                throw new PersistenceException(e);
            }
        });
    }

    @Override
    public CompletableFuture<List<BukkitCauldron>> findCauldrons(UUID worldUuid) {
        return ingredientManagerFuture.thenApplyAsync(ingredientManager -> {
            try (Connection connection = connectionSupplier.getUnchecked(); PreparedStatement preparedStatement = connection.prepareStatement(STATEMENTS.get(SqlStatements.Type.FIND))) {
                preparedStatement.setBytes(1, DecoderEncoder.asBytes(worldUuid));
                ResultSet resultSet = preparedStatement.executeQuery();
                List<BukkitCauldron> cauldrons = new ArrayList<>();
                while (resultSet.next()) {
                    int x = resultSet.getInt("cauldron_x");
                    int y = resultSet.getInt("cauldron_y");
                    int z = resultSet.getInt("cauldron_z");
                    Brew brew = BrewImpl.SERIALIZER.deserialize(JsonParser.parseString(resultSet.getString("brew")), ingredientManager);
                    String typeName = resultSet.getString("cauldron_type");
                    CauldronType cauldronType;
                    if (typeName != null) {
                        BreweryKey typeKey = BreweryKey.parse(typeName);
                        cauldronType = Arrays.stream(CauldronType.values()).filter(cauldronType1 -> cauldronType1.key().equals(typeKey))
                                .findAny()
                                .orElse(fromBrew(brew));
                    } else {
                        cauldronType = fromBrew(brew);
                    }
                    cauldrons.add(new BukkitCauldron(
                            brew,
                            new BreweryLocation(x, y, z, worldUuid),
                            cauldronType
                    ));
                }
                return cauldrons;
            } catch (SQLException e) {
                throw new UncheckedPersistenceException(e);
            }
        }, executor);
    }

    private CauldronType fromBrew(Brew brew) {
        return brew.lastStep() instanceof BrewingStep.CauldronStep<?> cauldronStep ?
                cauldronStep.cauldronType() : CauldronType.WATER;
    }
}
