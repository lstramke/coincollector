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

public class EuroCoinCollectionSqliteRepository implements EuroCoinCollectionStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionSqliteRepository.class);
    private final String tableName;
    private final EuroCoinCollectionFactory euroCoinCollectionFactory;

    public EuroCoinCollectionSqliteRepository(String tableName, EuroCoinCollectionFactory euroCoinCollectionFactory) {
        this.tableName = tableName;
        this.euroCoinCollectionFactory = euroCoinCollectionFactory;
    }

    @Override
    public void create(Connection connection, EuroCoinCollection collection) throws SQLException{
        if (!validateEuroCoinCollection(collection)) {
            logger.warn("EuroCoinCollection create aborted: validation failed");
            throw new IllegalArgumentException("EuroCoinCollection validation failed (create)");
        }

        String sql = String.format(
                "INSERT INTO %s (collection_id, name, group_id) VALUES (?, ?, ?)", 
                tableName);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, collection.getId());
            statement.setString(2, collection.getName());
            statement.setString(3, collection.getGroupId());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollection created: id={}, groupId={}", collection.getId(), collection.getGroupId());
            } else {
                logger.warn("EuroCoinCollection not created (rowsAffected={}): id={}, groupId={}", rowsAffected, collection.getId(), collection.getGroupId());
                throw new SQLException("EuroCoinCollection create affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection create failed: id={}, groupId={}", collection.getId(), collection.getGroupId(), e);
            throw e;
        }
    }

    @Override
    public Optional<EuroCoinCollection> read(Connection connection, String id) throws SQLException {
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoinCollection read aborted: id null/blank");
            return Optional.empty();
        }

        String sql = String.format(
            """
            SELECT collection_id, name, group_id
            FROM %s
            WHERE collection_id = ?
            """, tableName
        );

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet queryResult = statement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinCollectionFromResultSet(id, queryResult);
                } else {
                    logger.debug("EuroCoinCollection not found: id={}", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection read failed: id={}", id, e);
            throw e;
        }
    }

    private Optional<EuroCoinCollection> createEuroCoinCollectionFromResultSet(String id, ResultSet queryResult) {
        try {
            EuroCoinCollection readCollection = euroCoinCollectionFactory.fromDataBaseEntry(queryResult);
            logger.debug("EuroCoinCollection read: id={}, groupId={}", id, readCollection.getGroupId());
            return Optional.of(readCollection);
        } catch (SQLException e) {
            logger.warn("EuroCoinCollection read produced invalid data: id={}", id);
            return Optional.empty();
        }
    }

    @Override
    public void update(Connection connection, EuroCoinCollection collection) throws SQLException {
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

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, collection.getName());
            statement.setString(2, collection.getGroupId());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollection updated: id={}, groupId={}", collection.getId(), collection.getGroupId());
            } else {
                logger.warn("EuroCoinCollection not updated (rowsAffected={}): id={}, groupId={}", rowsAffected, collection.getId(), collection.getGroupId());
                throw new SQLException("EuroCoinCollection update affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection update failed: id={}, groupId={}", collection.getId(), collection.getGroupId(), e);
            throw e;
        }
    }

    @Override
    public void delete(Connection connection, String id) throws SQLException {
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoinCollection delete aborted: id null/blank");
            throw new IllegalArgumentException("collectionId must not be null or blank (delete)");
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE collection_id = ?
            """, tableName
        );

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 1) {
                logger.info("EuroCoinCollection deleted: id={}", id);
            } else {
                logger.warn("EuroCoinCollection not deleted (rowsAffected={}): id={}", rowsAffected, id);
                throw new SQLException("EuroCoinCollection delete affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection delete failed: id={}", id, e);
            throw e;
        }
    }

    @Override
    public List<EuroCoinCollection> getAll(Connection connection) throws SQLException {
        String sql = String.format(
            """
            SELECT collection_id, name, group_id
            FROM %s
            """, tableName
        );

        List<EuroCoinCollection> readCollections = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try(ResultSet resultSet = statement.executeQuery()){
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

    @Override
    public boolean exists(Connection connection, String id) throws SQLException {
        if (id == null || id.isBlank()) {
            logger.warn("EuroCoinCollection exists check aborted: id null/blank");
            throw new IllegalArgumentException("collectionId must not be null or blank (exists)");
        }

        String sql = String.format(
            """
            SELECT 1
            FROM %s
            WHERE collection_id = ?
            """, tableName
        );

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            logger.error("EuroCoinCollection exists check failed: id={}", id, e);
            throw e;
        }
    }

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
