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

public class EuroCoinCollectionGroupSqliteRepository implements EuroCoinCollectionGroupStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionGroupSqliteRepository.class);
    private final String tableName;
    private final EuroCoinCollectionGroupFactory euroCoinCollectionGroupFactory;

    public EuroCoinCollectionGroupSqliteRepository(String tableName, EuroCoinCollectionGroupFactory euroCoinCollectionGroupFactory) {
        this.tableName = tableName;
        this.euroCoinCollectionGroupFactory = euroCoinCollectionGroupFactory;
    }

    @Override
    public void create(Connection connection, EuroCoinCollectionGroup group) throws SQLException {
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
                logger.info("EuroCoinCollectionGroup created: id={}, ownerId={}", group.getId(), group.getOwnerId());
            } else {
                logger.warn("EuroCoinCollectionGroup not created (rowsAffected={}): id={}, ownerId={}", rowsAffected, group.getId(), group.getOwnerId());
                throw new SQLException("EuroCoinCollectionGroup create affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup create failed: id={}, ownerId={}", group.getId(), group.getOwnerId(), e);
            throw e;
        }
    }

    @Override
    public Optional<EuroCoinCollectionGroup> read(Connection connection, String id) throws SQLException {
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoinCollectionGroup read aborted: id null/blank");
            return Optional.empty();
        }

        String sql = String.format(
            """
            SELECT group_id, name, owner_id
            FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            try (ResultSet queryResult = preparedStatement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinCollectionGroupFromResultSet(id, queryResult);
                } else {
                    logger.debug("EuroCoinCollectionGroup not found: id={}", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup read failed: id={}", id, e);
            throw e;
        }
    }

    private Optional<EuroCoinCollectionGroup> createEuroCoinCollectionGroupFromResultSet(String id, ResultSet queryResult) {
        try {
            EuroCoinCollectionGroup readCollection = euroCoinCollectionGroupFactory.fromDataBaseEntry(queryResult);
            logger.debug("EuroCoinCollectionGroup read: id={}, ownerId={}", id, readCollection.getOwnerId());
            return Optional.of(readCollection);
        } catch (SQLException e) {
            logger.warn("EuroCoinCollectionGroup read produced invalid data: id={}", id);
            return Optional.empty();
        }
    }

    @Override
    public void update(Connection connection, EuroCoinCollectionGroup group) throws SQLException {
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
                logger.info("EuroCoinCollectionGroup updated: id={}, ownerId={}", group.getId(), group.getOwnerId());
            } else {
                logger.warn("EuroCoinCollectionGroup not updated (rowsAffected={}): id={}, ownerId={}", rowsAffected, group.getId(), group.getOwnerId());
                throw new SQLException("EuroCoinCollectionGroup update affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup update failed: id={}, ownerId={}", group.getId(), group.getOwnerId(), e);
            throw e;
        }
    }

    @Override
    public void delete(Connection connection, String id) throws SQLException {
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoinCollectionGroup delete aborted: id null/blank");
            throw new IllegalArgumentException("groupId must not be null or blank (delete)");
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollectionGroup deleted: id={}", id);
            } else {
                logger.warn("EuroCoinCollectionGroup not deleted (rowsAffected={}): id={}", rowsAffected, id);
                throw new SQLException("EuroCoinCollectionGroup delete affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup delete failed: id={}", id, e);
            throw e;
        }
    }

    @Override
    public List<EuroCoinCollectionGroup> getAllByUser(Connection connection, String userId) throws SQLException {
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
                        logger.warn("EuroCoinCollectionGroup row skipped: id={} (invalid data)", groupId);
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

    @Override
    public boolean exists(Connection connection, String id) throws SQLException {
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoinCollectionGroup exists check aborted: id null/blank");
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
            preparedStatement.setString(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            logger.error("EuroCoinCollectionGroup exists check failed: id={}", id, e);
            throw e;
        }
    }

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