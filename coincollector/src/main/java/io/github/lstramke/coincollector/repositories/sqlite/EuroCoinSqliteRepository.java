package io.github.lstramke.coincollector.repositories.sqlite;

import io.github.lstramke.coincollector.model.CoinCountry;
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

/**
 * SQLite-backed implementation of {@link EuroCoinStorageRepository} providing CRUD
 * access to {@link EuroCoin} rows in a configurable table. Responsibilities:
 * <ul>
 *   <li>Create / read / update / delete / getAll coin records</li>
 *   <li>Map result sets to domain objects via {@link EuroCoinFactory}</li>
 *   <li>Basic invariant validation (id, year boundary, non-null enum/value fields)</li>
 * </ul>
 * This class does NOT manage transaction boundaries or connection lifecycle: callers
 * must pass an open {@link Connection}. JDBC resources are closed using
 * try-with-resources. Unexpected row counts in write operations raise a
 * {@link SQLException}.
 */
public class EuroCoinSqliteRepository implements EuroCoinStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinSqliteRepository.class);
    private final String tableName;
    private final EuroCoinFactory euroCoinFactory;

    public EuroCoinSqliteRepository(String tableName, EuroCoinFactory euroCoinFactory) {
        this.tableName = tableName;
        this.euroCoinFactory = euroCoinFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void create(Connection connection, EuroCoin coin) throws SQLException{
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (create)");
        }
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
            preparedStatement.setString(5, coin.getMintCountry().equals(CoinCountry.GERMANY) ? coin.getMint().getMintMark() : null);
            preparedStatement.setString(6, coin.getDescription().getText());
            preparedStatement.setString(7, coin.getCollectionId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 1) {
                logger.info("EuroCoin created: coinId={}, collectionId={}", coin.getId(), coin.getCollectionId());
            } else {
                logger.warn("EuroCoin not created (rowsAffected={}): coinId={}, collectionId={}", rowsAffected, coin.getId(), coin.getCollectionId());
                throw new SQLException("EuroCoin create affected unexpected number of rows: " + rowsAffected);
            }


        } catch (SQLException e) {
            logger.error("EuroCoin create failed: coinId={}, collectionId={}", coin.getId(), coin.getCollectionId(), e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<EuroCoin> read(Connection connection, String coinId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (read)");
        }
        if (coinId == null || coinId.isBlank()) {
            logger.warn("EuroCoin read aborted: coinId null/blank");
            throw new IllegalArgumentException("coinId must not be null or blank (read)");
        }

        String sql = String.format(
            """ 
            SELECT coin_id, year, coin_value, mint_country, mint, description, collection_id
            FROM %s
            WHERE coin_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, coinId);
            try (ResultSet queryResult = preparedStatement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinFromResultSet(coinId, queryResult);
                } else {
                    logger.debug("EuroCoin not found: coinId={}", coinId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("EuroCoin read failed: coinId={}", coinId, e);
            throw e;
        }
    }

    /**
     * Maps the current row of the given {@link ResultSet} to a {@link EuroCoin} using
     * the {@link EuroCoinFactory}. Returns an {@link Optional} that is present unless
     * mapping fails with a {@link SQLException}, in which case an empty Optional is
     * returned and a warning is logged. Returning {@link Optional#empty()} on mapping
     * failure enables a best-effort "get as much as you can" strategy in
     * {@link #getAll(Connection)} so that a single corrupt row does not abort reading
     * the remaining valid rows.
     *
     * @param coinId id used only for logging correlation
     * @param queryResult result set positioned on the current row
     * @return optional containing the mapped coin, or empty if mapping failed
     */
    private Optional<EuroCoin> createEuroCoinFromResultSet(String coinId, ResultSet queryResult) {
        try {
            EuroCoin readCoin = euroCoinFactory.fromDataBaseEntry(queryResult);
            logger.debug("EuroCoin read: coinId={}, collectionId={}", coinId, readCoin.getCollectionId());
            return Optional.of(readCoin);
        } catch (SQLException e) {
            logger.warn("EuroCoin read produced invalid data: coinId={}", coinId);
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(Connection connection, EuroCoin coin) throws SQLException{
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (update)");
        }
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
            preparedStatement.setString(4, coin.getMintCountry().equals(CoinCountry.GERMANY) ? coin.getMint().getMintMark() : null);
            preparedStatement.setString(5, coin.getDescription().getText());
            preparedStatement.setString(6, coin.getCollectionId());
            preparedStatement.setString(7, coin.getId());

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoin updated: coinId={}, collectionId={}", coin.getId(), coin.getCollectionId());
            } else {
                logger.warn("EuroCoin not updated (rowsAffected={}): coinId={}, collectionId={}", rowsAffected, coin.getId(), coin.getCollectionId());
                throw new SQLException("EuroCoin update affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoin update failed: coinId={}, collectionId={}", coin.getId(), coin.getCollectionId(), e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Connection connection, String coinId)  throws SQLException{
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (delete)");
        }
        if (coinId == null || coinId.isBlank()) {
            logger.warn("EuroCoin delete aborted: coinId null/blank");
            throw new IllegalArgumentException("coinId must not be null or blank (delete)");
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE coin_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, coinId);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoin deleted: coinId={}", coinId);
            } else {
                logger.warn("EuroCoin not deleted (rowsAffected={}): coinId={}", rowsAffected, coinId);
                throw new SQLException("EuroCoin delete affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoin delete failed: coinId={}", coinId, e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoin> getAll(Connection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (getAll)");
        }
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
                        logger.warn("EuroCoin row skipped: coinId={} (invalid data)", coinId);
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

    /** {@inheritDoc} */
    @Override
    public boolean exists(Connection connection, String coinId) throws SQLException{
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (exists)");
        }
        if (coinId == null || coinId.isBlank()) {
            logger.warn("EuroCoin exists check aborted: coinId null/blank");
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
            preparedStatement.setString(1, coinId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("EuroCoin exists check failed: coinId={}", coinId, e);
            throw e;
        }
    }

    /**
     * Internal (package-private) validation of minimal {@link EuroCoin} invariants.
     * Current rules: non-null object, non-blank id, year >= EURO_COIN_START_YEAR and
     * non-null mandatory fields (value, mint country, mint, collectionId not blank).
     * Extend here if domain constraints evolve.
     *
     * @param coin coin domain object to validate
     * @return true if all rules pass; false otherwise
     */
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

        if (coin.getMintCountry().equals(CoinCountry.GERMANY) && coin.getMint() == null) {
            return false;
        }

        if (coin.getCollectionId() == null || coin.getCollectionId().isBlank()) {
            return false;
        }

        return true;
    }
}
