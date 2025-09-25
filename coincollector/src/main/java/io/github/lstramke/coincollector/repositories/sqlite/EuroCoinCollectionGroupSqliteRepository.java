package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroupFactory;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionGroupStorageRepository;

/**
 * SQLite-backed implementation of {@link EuroCoinCollectionGroupStorageRepository} providing CRUD
 * access to {@link EuroCoinCollectionGroup} rows in a configurable table. Responsibilities:
 * <ul>
 *   <li>Create / read / update / delete / getAllByUser group records</li>
 *   <li>Map result sets to domain objects via {@link EuroCoinCollectionFactory}</li>
 *   <li>Basic invariant validation (id, group id, non-null collections list)</li>
 * </ul>
 * This class does NOT manage transaction boundaries or connection lifecycle: callers
 * must pass an open {@link java.sql.Connection}. JDBC resources are closed using
 * try-with-resources. Unexpected row counts in write operations raise a
 * {@link SQLException}. Returning partial results on read list operations is preferred;
 * corrupt rows (mapping failures) are skipped with a warning so that remaining valid
 * rows are still returned.
 */
public class EuroCoinCollectionGroupSqliteRepository implements EuroCoinCollectionGroupStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionGroupSqliteRepository.class);
    private final String tableName;
    private final EuroCoinCollectionGroupFactory euroCoinCollectionGroupFactory;

    public EuroCoinCollectionGroupSqliteRepository(String tableName, EuroCoinCollectionGroupFactory euroCoinCollectionGroupFactory) {
        this.tableName = tableName;
        this.euroCoinCollectionGroupFactory = euroCoinCollectionGroupFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void create(Connection connection, EuroCoinCollectionGroup group) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (create)");
        }
        if(!validateEuroCoinCollectionGroup(group)){
            logger.warn("EuroCoinCollectionGroup create aborted: validation failed");
            throw new IllegalArgumentException("EuroCoinCollectionGroup validation failed (create)");
        }

        String sql = String.format(
            "INSERT INTO %s (group_id, name, owner_id) VALUES (?, ?, ?)", 
            tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, group.getId());
            preparedStatement.setString(2, group.getName());
            preparedStatement.setString(3, group.getOwnerId());

            int rowsAffected = preparedStatement.executeUpdate();
            if(rowsAffected == 1){
                logger.info("EuroCoinCollectionGroup created: groupId={}, ownerId={}", group.getId(), group.getOwnerId());
            } else {
                logger.warn("EuroCoinCollectionGroup not created (rowsAffected={}): groupId={}, ownerId={}", rowsAffected, group.getId(), group.getOwnerId());
                throw new SQLException("EuroCoinCollectionGroup create affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup create failed: groupId={}, ownerId={}", group.getId(), group.getOwnerId(), e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<EuroCoinCollectionGroup> read(Connection connection, String groupId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (read)");
        }
        if (groupId == null || groupId.isBlank()) {
            logger.warn("EuroCoinCollectionGroup read aborted: groupId null/blank");
            throw new IllegalArgumentException("groupId must not be null or blank (read)");
        }

        String sql = String.format(
            """
            SELECT group_id, name, owner_id
            FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, groupId);
            try (ResultSet queryResult = preparedStatement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinCollectionGroupFromResultSet(groupId, queryResult);
                } else {
                    logger.debug("EuroCoinCollectionGroup not found: groupId={}", groupId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup read failed: groupId={}", groupId, e);
            throw e;
        }
    }

    /**
     * Maps the current row of the given {@link ResultSet} to a {@link EuroCoinCollectionGroup} using
     * the {@link EuroCoinCollectionGroupFactory}. Returns an {@link Optional} that is present unless
     * mapping fails with a {@link SQLException}, in which case an empty Optional is returned and
     * a warning is logged. Returning {@link Optional#empty()} on mapping failure enables a
     * best-effort "get as much as you can" strategy in {@link #getAllByUser(Connection, String)} so that a
     * single corrupt row does not abort reading the remaining valid rows.
     *
     * @param groupId id used only for logging correlation
     * @param queryResult result set positioned on the current row
     * @return optional containing the mapped group, or empty if mapping failed
     */
    private Optional<EuroCoinCollectionGroup> createEuroCoinCollectionGroupFromResultSet(String groupId, ResultSet queryResult) {
        try {
            EuroCoinCollectionGroup readCollection = euroCoinCollectionGroupFactory.fromDataBaseEntry(queryResult);
            logger.debug("EuroCoinCollectionGroup read: groupId={}, ownerId={}", groupId, readCollection.getOwnerId());
            return Optional.of(readCollection);
        } catch (SQLException e) {
            logger.warn("EuroCoinCollectionGroup read produced invalid data: groupId={}", groupId);
            return Optional.empty();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(Connection connection, EuroCoinCollectionGroup group) throws SQLException {
       if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (update)");
        }
       if (!validateEuroCoinCollectionGroup(group)) {
            logger.warn("EuroCoinCollectionGroup update aborted: validation failed");
            throw new IllegalArgumentException("EuroCoinCollectionGroup validation failed (update)");
        }

        String sql = String.format(
            """
            UPDATE %s
            SET name = ?, owner_id = ?
            WHERE group_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, group.getName());
            preparedStatement.setString(2, group.getOwnerId());
            preparedStatement.setString(3, group.getId());

            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollectionGroup updated: groupId={}, ownerId={}", group.getId(), group.getOwnerId());
            } else {
                logger.warn("EuroCoinCollectionGroup not updated (rowsAffected={}): groupId={}, ownerId={}", rowsAffected, group.getId(), group.getOwnerId());
                throw new SQLException("EuroCoinCollectionGroup update affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup update failed: groupId={}, ownerId={}", group.getId(), group.getOwnerId(), e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Connection connection, String groupId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (delete)");
        }
        if (groupId == null || groupId.isBlank()) {
            logger.warn("EuroCoinCollectionGroup delete aborted: groupId null/blank");
            throw new IllegalArgumentException("groupId must not be null or blank (delete)");
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, groupId);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollectionGroup deleted: groupId={}", groupId);
            } else {
                logger.warn("EuroCoinCollectionGroup not deleted (rowsAffected={}): groupId={}", rowsAffected, groupId);
                throw new SQLException("EuroCoinCollectionGroup delete affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup delete failed: groupId={}", groupId, e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoinCollectionGroup> getAllByUser(Connection connection, String userId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (getAllByUser)");
        }
        if (userId == null || userId.isBlank()) {
            logger.warn("EuroCoinCollectionGroup list read aborted: ownerId null/blank");
            throw new IllegalArgumentException("ownerId must not be null or blank (list)");
        }

        String sql = String.format(
            """
            SELECT group_id, name, owner_id
            FROM %s
            WHERE owner_id = ?
            """, tableName
        );

        List<EuroCoinCollectionGroup> readCollections = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()){
                while (resultSet.next()) {
                    String groupId = resultSet.getString("group_id");
                    Optional<EuroCoinCollectionGroup> readCollection = createEuroCoinCollectionGroupFromResultSet(groupId, resultSet);
                    if (readCollection.isPresent()) {
                        readCollections.add(readCollection.get());
                    } else {
                        logger.warn("EuroCoinCollectionGroup row skipped: groupId={} (invalid data)", groupId);
                    }
                }
            }
            logger.debug("EuroCoinCollectionGroup list read: count={}, ownerId={}", readCollections.size(), userId);
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup list read failed: ownerId={}", userId, e);
            throw e;
        }

        return readCollections;
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(Connection connection, String groupId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (exists)");
        }
        if (groupId == null || groupId.isBlank()) {
            logger.warn("EuroCoinCollectionGroup exists check aborted: groupId null/blank");
            throw new IllegalArgumentException("groupId must not be null or blank (exists)");
        }

        String sql = String.format(
            """
            SELECT 1
            FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, groupId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup exists check failed: groupId={}", groupId, e);
            throw e;
        }
    }

    /**
     * Internal (package-private) validation of minimal {@link EuroCoinCollectionGroup} invariants.
     * Current rules: non-null object, non-blank group id, non-blank owner id and non-null collections
     * list (may be empty). Extend here if domain constraints evolve.
     *
     * @param group group aggregate to validate
     * @return true if all rules pass; false otherwise
     */
    boolean validateEuroCoinCollectionGroup(EuroCoinCollectionGroup group){
        if(group == null){
            return false;
        }

        if(group.getId() == null || group.getId().isBlank()){
            return false;
        }

        if(group.getOwnerId() == null || group.getOwnerId().isBlank()){
            return false;
        }

        if(group.getCollections() == null){
            return false;
        }

        return true;
    }
}