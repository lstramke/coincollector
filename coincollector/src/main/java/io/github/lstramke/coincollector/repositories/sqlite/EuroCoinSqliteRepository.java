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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EuroCoinSqliteRepository implements EuroCoinStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinSqliteRepository.class);
    private final String tableName;
    private final EuroCoinFactory euroCoinFactory;

    public EuroCoinSqliteRepository(String tableName, EuroCoinFactory euroCoinFactory) {
        this.tableName = tableName;
        this.euroCoinFactory = euroCoinFactory;
    }

    @Override
    public void create(Connection connection, EuroCoin coin) throws SQLException{
        if (!validateEuroCoin(coin)) {
            logger.warn("EuroCoin create aborted: validation failed");
            throw new IllegalArgumentException("EuroCoin validation failed (create)");
        }

        String sql = String.format(
                "INSERT INTO %s (coin_id, year, coin_value, mint_country, mint, description, collection_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableName);

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, coin.getId());
            preparedStatement.setInt(2, coin.getYear());
            preparedStatement.setInt(3, coin.getValue().getCentValue());
            preparedStatement.setString(4, coin.getMintCountry().getIsoCode());
            preparedStatement.setString(5, coin.getMint().getMintMark());
            preparedStatement.setString(6, coin.getDescription().getText());
            preparedStatement.setString(7, coin.getCollectionId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 1) {
                logger.info("EuroCoin created: id={}, collectionId={}", coin.getId(), coin.getCollectionId());
            } else {
                logger.warn("EuroCoin not created (rowsAffected={}): id={}, collectionId={}", rowsAffected, coin.getId(), coin.getCollectionId());
                throw new SQLException("EuroCoin create affected unexpected number of rows: " + rowsAffected);
            }


        } catch (SQLException e) {
            logger.error("EuroCoin create failed: id={}, collectionId={}", coin.getId(), coin.getCollectionId(), e);
            throw e;
        }
    }

    @Override
    public Optional<EuroCoin> read(Connection connection, String id) throws SQLException {
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoin read aborted: id null/blank");
            return Optional.empty();
        }

        String sql = String.format(
            """ 
            SELECT coin_id, year, coin_value, mint_country, mint, description, collection_id
            FROM %s
            WHERE coin_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            try (ResultSet queryResult = preparedStatement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinFromResultSet(id, queryResult);
                } else {
                    logger.debug("EuroCoin not found: id={}", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("EuroCoin read failed: id={}", id, e);
            throw e;
        }
    }

    private Optional<EuroCoin> createEuroCoinFromResultSet(String id, ResultSet queryResult) {
        try {
            EuroCoin readCoin = euroCoinFactory.fromDataBaseEntry(queryResult);
            logger.debug("EuroCoin read: id={}, collectionId={}", id, readCoin.getCollectionId());
            return Optional.of(readCoin);
        } catch (SQLException e) {
            logger.warn("EuroCoin read produced invalid data: id={}", id);
            return Optional.empty();
        }
    }

    @Override
    public void update(Connection connection, EuroCoin coin) throws SQLException{
        if (!validateEuroCoin(coin)) {
            logger.warn("EuroCoin update aborted: validation failed");
            throw new IllegalArgumentException("EuroCoin validation failed (update)");
        }

        String sql = String.format(
            """
            UPDATE %s
            SET year = ?, coin_value = ?, mint_country = ?, mint = ?, description = ?, collection_id = ?
            WHERE coin_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, coin.getYear());
            preparedStatement.setInt(2, coin.getValue().getCentValue());
            preparedStatement.setString(3, coin.getMintCountry().getIsoCode());
            preparedStatement.setString(4, coin.getMint().getMintMark());
            preparedStatement.setString(5, coin.getDescription().getText());
            preparedStatement.setString(6, coin.getCollectionId());
            preparedStatement.setString(7, coin.getId());

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoin updated: id={}, collectionId={}", coin.getId(), coin.getCollectionId());
            } else {
                logger.warn("EuroCoin not updated (rowsAffected={}): id={}, collectionId={}", rowsAffected, coin.getId(), coin.getCollectionId());
                throw new SQLException("EuroCoin update affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoin update failed: id={}, collectionId={}", coin.getId(), coin.getCollectionId(), e);
            throw e;
        }
    }

    @Override
    public void delete(Connection connection, String id)  throws SQLException{
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoin delete aborted: id null/blank");
            throw new IllegalArgumentException("coinId must not be null or blank (delete)");
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE coin_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoin deleted: id={}", id);
            } else {
                logger.warn("EuroCoin not deleted (rowsAffected={}): id={}", rowsAffected, id);
                throw new SQLException("EuroCoin delete affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoin delete failed: id={}", id, e);
            throw e;
        }
    }

    @Override
    public List<EuroCoin> getAll(Connection connection) throws SQLException {
        String sql = String.format(
            """
            SELECT coin_id, year, coin_value, mint_country, mint, description, collection_id
            FROM %s
            """, tableName
        );

        List<EuroCoin> readCoins = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            try(ResultSet rs = preparedStatement.executeQuery()){
                while (rs.next()) {
                    String coinId = rs.getString("coin_id");
                    Optional<EuroCoin> readCoin = createEuroCoinFromResultSet(coinId, rs);
                    if (readCoin.isPresent()) {
                        readCoins.add(readCoin.get());
                    } else {
                        logger.warn("EuroCoin row skipped: id={} (invalid data)", coinId);
                    }
                }
                logger.debug("EuroCoin list read: count={}", readCoins.size());
            }
        } catch (SQLException e) {
            logger.error("EuroCoin list read failed", e);
            throw e;
        }
        return readCoins;
    }

    @Override
    public boolean exists(Connection connection, String id) throws SQLException{
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoin exists check aborted: id null/blank");
            throw new IllegalArgumentException("coinId must not be null or blank (exists)");
        }

        String sql = String.format(
            """
            SELECT 1
            FROM %s
            WHERE coin_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("EuroCoin exists check failed: id={}", id, e);
            throw e;
        }
    }

    boolean validateEuroCoin(EuroCoin coin) {
        if (coin == null) {
            return false;
        }

        if (coin.getId() == null || coin.getId().isBlank()) {
            return false;
        }

        if (coin.getYear() < EuroCoinBuilder.EURO_COIN_START_YEAR) {
            return false;
        }

        if (coin.getValue() == null) {
            return false;
        }

        if (coin.getMintCountry() == null) {
            return false;
        }

        if (coin.getMint() == null) {
            return false;
        }

        if (coin.getCollectionId() == null || coin.getCollectionId().isBlank()) {
            return false;
        }

        return true;
    }
}
