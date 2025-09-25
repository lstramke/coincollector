package io.github.lstramke.coincollector.repositories.sqlite;

import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionFactory;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionStorageRepository;
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
 * SQLite-backed implementation of {@link EuroCoinCollectionStorageRepository} providing CRUD
 * access to {@link EuroCoinCollection} rows in a configurable table. Responsibilities:
 * <ul>
 *   <li>Create / read / update / delete / getAll collection records</li>
 *   <li>Map result sets to domain objects via {@link EuroCoinCollectionFactory}</li>
 *   <li>Basic invariant validation (id, group id, non-null coins list)</li>
 * </ul>
 * This class does NOT manage transaction boundaries or connection lifecycle: callers
 * must pass an open {@link java.sql.Connection}. JDBC resources are closed using
 * try-with-resources. Unexpected row counts in write operations raise a
 * {@link SQLException}. Returning partial results on read list operations is preferred;
 * corrupt rows (mapping failures) are skipped with a warning so that remaining valid
 * rows are still returned.
 */
public class EuroCoinCollectionSqliteRepository implements EuroCoinCollectionStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionSqliteRepository.class);
    private final String tableName;
    private final EuroCoinCollectionFactory euroCoinCollectionFactory;

    public EuroCoinCollectionSqliteRepository(String tableName, EuroCoinCollectionFactory euroCoinCollectionFactory) {
        this.tableName = tableName;
        this.euroCoinCollectionFactory = euroCoinCollectionFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void create(Connection connection, EuroCoinCollection collection) throws SQLException{
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (create)");
        }
        if (!validateEuroCoinCollection(collection)) {
            logger.warn("EuroCoinCollection create aborted: validation failed");
            throw new IllegalArgumentException("EuroCoinCollection validation failed (create)");
        }

        String sql = String.format(
                "INSERT INTO %s (collection_id, name, group_id) VALUES (?, ?, ?)", 
                tableName);

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, collection.getId());
            preparedStatement.setString(2, collection.getName());
            preparedStatement.setString(3, collection.getGroupId());

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollection created: collectionId={}, groupId={}", collection.getId(), collection.getGroupId());
            } else {
                logger.warn("EuroCoinCollection not created (rowsAffected={}): collectionId={}, groupId={}", rowsAffected, collection.getId(), collection.getGroupId());
                throw new SQLException("EuroCoinCollection create affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection create failed: collectionId={}, groupId={}", collection.getId(), collection.getGroupId(), e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<EuroCoinCollection> read(Connection connection, String collectionId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (read)");
        }
        if (collectionId == null || collectionId.isBlank()) {
            logger.warn("EuroCoinCollection read aborted: collectionId null/blank");
            throw new IllegalArgumentException("collectionId must not be null or blank (read)");
        }

        String sql = String.format(
            """
            SELECT collection_id, name, group_id
            FROM %s
            WHERE collection_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, collectionId);
            try (ResultSet queryResult = preparedStatement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinCollectionFromResultSet(collectionId, queryResult);
                } else {
                    logger.debug("EuroCoinCollection not found: collectionId={}", collectionId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection read failed: collectionId={}", collectionId, e);
            throw e;
        }
    }

    /**
     * Maps the current row of the given {@link ResultSet} to a {@link EuroCoinCollection} using
     * the {@link EuroCoinCollectionFactory}. Returns an {@link Optional} that is present unless
     * mapping fails with a {@link SQLException}, in which case an empty Optional is returned and
     * a warning is logged. Returning {@link Optional#empty()} on mapping failure enables a
     * best-effort "get as much as you can" strategy in {@link #getAll(Connection)} so that a
     * single corrupt row does not abort reading the remaining valid rows.
     *
     * @param collectionId id used only for logging correlation
     * @param queryResult result set positioned on the current row
     * @return optional containing the mapped collection, or empty if mapping failed
     */
    private Optional<EuroCoinCollection> createEuroCoinCollectionFromResultSet(String collectionId, ResultSet queryResult) {
        try {
            EuroCoinCollection readCollection = euroCoinCollectionFactory.fromDataBaseEntry(queryResult);
            logger.debug("EuroCoinCollection read: collectionId={}, groupId={}", collectionId, readCollection.getGroupId());
            return Optional.of(readCollection);
        } catch (SQLException e) {
            logger.warn("EuroCoinCollection read produced invalid data: collectionId={}", collectionId);
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(Connection connection, EuroCoinCollection collection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (update)");
        }
        if (!validateEuroCoinCollection(collection)) {
            logger.warn("EuroCoinCollection update aborted: validation failed");
            throw new IllegalArgumentException("EuroCoinCollection validation failed (update)");
        }

        String sql = String.format(
            """
            UPDATE %s
            SET name = ?, group_id = ?
            WHERE collection_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, collection.getName());
            preparedStatement.setString(2, collection.getGroupId());
            preparedStatement.setString(3, collection.getId());

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollection updated: collectionId={}, groupId={}", collection.getId(), collection.getGroupId());
            } else {
                logger.warn("EuroCoinCollection not updated (rowsAffected={}): collectionId={}, groupId={}", rowsAffected, collection.getId(), collection.getGroupId());
                throw new SQLException("EuroCoinCollection update affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection update failed: collectionId={}, groupId={}", collection.getId(), collection.getGroupId(), e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Connection connection, String collectionId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (delete)");
        }
        if (collectionId == null || collectionId.isBlank()) {
            logger.warn("EuroCoinCollection delete aborted: collectionId null/blank");
            throw new IllegalArgumentException("collectionId must not be null or blank (delete)");
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE collection_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, collectionId);

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollection deleted: collectionId={}", collectionId);
            } else {
                logger.warn("EuroCoinCollection not deleted (rowsAffected={}): collectionId={}", rowsAffected, collectionId);
                throw new SQLException("EuroCoinCollection delete affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection delete failed: collectionId={}", collectionId, e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoinCollection> getAll(Connection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (getAll)");
        }
        String sql = String.format(
            """
            SELECT collection_id, name, group_id
            FROM %s
            """, tableName
        );

        List<EuroCoinCollection> readCollections = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                while (resultSet.next()) {
                String collectionId = resultSet.getString("collection_id");
                Optional<EuroCoinCollection> readCollection = createEuroCoinCollectionFromResultSet(collectionId, resultSet);
                if (readCollection.isPresent()) {
                    readCollections.add(readCollection.get());
                } else {
                    logger.warn("Skipping EuroCoinCollection row (collection_id={}) – invalid or incomplete data (validation failed)",
                            collectionId);
                }
            }
            logger.debug("EuroCoinCollection list read: count={}", readCollections.size());
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection list read failed", e);
            throw e;
        }

        return readCollections;
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(Connection connection, String collectionId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (exists)");
        }
        if (collectionId == null || collectionId.isBlank()) {
            logger.warn("EuroCoinCollection exists check aborted: collectionId null/blank");
            throw new IllegalArgumentException("collectionId must not be null or blank (exists)");
        }

        String sql = String.format(
            """
            SELECT 1
            FROM %s
            WHERE collection_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, collectionId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection exists check failed: collectionId={}", collectionId, e);
            throw e;
        }
    }

    /**
     * Internal (package-private) validation of minimal {@link EuroCoinCollection} invariants.
     * Current rules: non-null object, non-blank id, non-blank group id and non-null coins
     * collection (may be empty). Extend here if domain constraints evolve.
     *
     * @param collection collection aggregate to validate
     * @return true if all rules pass; false otherwise
     */
    boolean validateEuroCoinCollection(EuroCoinCollection collection) {
        if (collection == null) {
            return false;
        }

        if (collection.getId() == null || collection.getId().isBlank()) {
            return false;
        }

        if (collection.getGroupId() == null || collection.getGroupId().isBlank()) {
            return false;
        }

        if (collection.getCoins() == null) {
            return false;
        }

        return true;
    }
}
