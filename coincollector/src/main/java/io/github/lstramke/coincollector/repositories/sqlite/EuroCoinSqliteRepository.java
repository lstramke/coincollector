package io.github.lstramke.coincollector.repositories.sqlite;

import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.EuroCoinFactory;
import io.github.lstramke.coincollector.repositories.EuroCoinStorageRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EuroCoinSqliteRepository implements EuroCoinStorageRepository {

    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinSqliteRepository.class);
    private final String tableName;
    private final EuroCoinFactory euroCoinFactory;

    public EuroCoinSqliteRepository(DataSource dataSource, String tableName,
            EuroCoinFactory euroCoinFactory) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.euroCoinFactory = euroCoinFactory;
    }

    @Override
    public boolean create(EuroCoin coin) {
        if (!validateEuroCoin(coin)) {
            logger.warn("Cannot create EuroCoin - validation failed");
            return false;
        }

        String sql = String.format(
                "INSERT INTO %s (coin_id, year, coin_value, mint_country, mint, description, collection_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableName);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, coin.getId());
            statement.setInt(2, coin.getYear());
            statement.setInt(3, coin.getValue().getCentValue());
            statement.setString(4, coin.getMintCountry().getIsoCode());
            statement.setString(5, coin.getMint().getMintMark());
            statement.setString(6, coin.getDescription().getText());
            statement.setString(7, coin.getCollectionId());

            int rowsAffected = statement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully created EuroCoin with id: {} in collection: {}", coin.getId(), coin.getCollectionId());
            } else {
                logger.warn("Failed to create EuroCoin with id: {} in collection: {}", coin.getId(), coin.getCollectionId());
            }

            return success;

        } catch (SQLException e) {
            logger.error("Error creating EuroCoin with id: {}", coin.getId(), e);
            return false;
        }
    }

    @Override
    public Optional<EuroCoin> read(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot read EuroCoin - id is null or blank");
            return Optional.empty();
        }

        String sql = String.format("""
                SELECT coin_id, year, coin_value, mint_country, mint, description, collection_id
                FROM %s
                WHERE coin_id = ?
                """, tableName);

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            try (ResultSet queryResult = statement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinFromResultSet(id, queryResult);
                } else {
                    logger.debug("EuroCoin with id: {} not found", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error reading EuroCoin with id: {}", id, e);
            return Optional.empty();
        }
    }

    private Optional<EuroCoin> createEuroCoinFromResultSet(String id, ResultSet queryResult) {
        try {
            EuroCoin readCoin = euroCoinFactory.fromDataBaseEntry(queryResult);
            logger.debug("Successfully read EuroCoin with id: {}", id);
            return Optional.of(readCoin);
        } catch (SQLException e) {
            logger.warn("Invalid data for EuroCoin with id: {} - {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean update(EuroCoin coin) {
        if (!validateEuroCoin(coin)) {
            logger.warn("Cannot update EuroCoin - validation failed");
            return false;
        }

        String sql = String.format(
                """
                        UPDATE %s
                        SET year = ?, coin_value = ?, mint_country = ?, mint = ?, description = ?, collection_id = ?
                        WHERE coin_id = ?
                        """,
                tableName);

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, coin.getYear());
            statement.setInt(2, coin.getValue().getCentValue());
            statement.setString(3, coin.getMintCountry().getIsoCode());
            statement.setString(4, coin.getMint().getMintMark());
            statement.setString(5, coin.getDescription().getText());
            statement.setString(6, coin.getCollectionId());
            statement.setString(7, coin.getId());

            int rowsAffected = statement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully updated EuroCoin with id: {} in collection: {}",
                        coin.getId(), coin.getCollectionId());
            } else {
                logger.warn("Failed to update EuroCoin with id: {} in collection: {}", coin.getId(),
                        coin.getCollectionId());
            }

            return success;

        } catch (SQLException e) {
            logger.error("Error updating EuroCoin with id: {}", coin.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot delete EuroCoin - id is null or blank");
            return false;
        }

        String sql = String.format("""
                DELETE FROM %s
                WHERE coin_id = ?
                """, tableName);

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            int rowsAffected = statement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully deleted EuroCoin with id: {}", id);
            } else {
                logger.warn("Failed to delete EuroCoin with id: {}", id);
            }

            return success;
        } catch (SQLException e) {
            logger.error("Error deleting EuroCoin with id: {}", id, e);
            return false;
        }
    }

    @Override
    public List<EuroCoin> getAll() {
        String sql = String.format("""
                SELECT coin_id, year, coin_value, mint_country, mint, description, collection_id
                FROM %s
                """, tableName);

        List<EuroCoin> readCoins = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String coinId = rs.getString("coin_id");
                Optional<EuroCoin> readCoin = createEuroCoinFromResultSet(coinId, rs);
                if (readCoin.isPresent()) {
                    readCoins.add(readCoin.get());
                } else {
                    logger.warn("Skipping EuroCoin row (coin_id={}) – invalid or incomplete data (validation failed)", coinId);
                }
            }
            logger.debug("Successfully retrieved {} EuroCoins from database", readCoins.size());
        } catch (SQLException e) {
            logger.error("Error retrieving all EuroCoins from database", e);
        }

        return readCoins;
    }

    @Override
    public Optional<Boolean> exists(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot check existence of EuroCoin - id is null or blank");
            return Optional.of(false);
        }

        String sql = String.format("""
                SELECT 1
                FROM %s
                WHERE coin_id = ?
                """, tableName);

        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return Optional.of(rs.next());
            }
        } catch (SQLException e) {
            logger.error("Error checking existence of EuroCoin with id: {}", id, e);
            return Optional.empty();
        }
    }

    boolean validateEuroCoin(EuroCoin coin) {
        if (coin == null) {
            logger.warn("Validation failed: EuroCoin is null");
            return false;
        }

        if (coin.getId() == null || coin.getId().isBlank()) {
            logger.warn("Validation failed: EuroCoin ID is null or blank");
            return false;
        }

        if (coin.getYear() < EuroCoinBuilder.EURO_COIN_START_YEAR) {
            logger.warn("Validation failed: Invalid year {} for EuroCoin {}", coin.getYear(),
                    coin.getId());
            return false;
        }

        if (coin.getValue() == null) {
            logger.warn("Validation failed: CoinValue is null for EuroCoin {}", coin.getId());
            return false;
        }

        if (coin.getMintCountry() == null) {
            logger.warn("Validation failed: MintCountry is null for EuroCoin {}", coin.getId());
            return false;
        }

        if (coin.getMint() == null) {
            logger.warn("Validation failed: Mint is null for EuroCoin {}", coin.getId());
            return false;
        }

        if (coin.getCollectionId() == null || coin.getCollectionId().isBlank()) {
            logger.warn("Validation failed: CollectionId is required for EuroCoin {} - no standalone readCoins allowed", coin.getId());
            return false;
        }

        logger.debug("Validation successful for EuroCoin {}", coin.getId());
        return true;
    }
}
