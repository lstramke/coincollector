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
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EuroCoinCollectionSqliteRepository implements EuroCoinCollectionStorageRepository {

    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory
            .getLogger(EuroCoinCollectionSqliteRepository.class);
    private final String tableName;
    private final EuroCoinCollectionFactory euroCoinCollectionFactory;

    public EuroCoinCollectionSqliteRepository(DataSource dataSource, String tableName,
            EuroCoinCollectionFactory euroCoinCollectionFactory) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.euroCoinCollectionFactory = euroCoinCollectionFactory;
    }

    @Override
    public boolean create(EuroCoinCollection collection) {
        if (!validateEuroCoinCollection(collection)) {
            logger.warn("Cannot create EuroCoinCollection - validation failed");
            return false;
        }

        String sql = String.format(
                "INSERT INTO %s (collection_id, name, group_id) VALUES (?, ?, ?)", tableName);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, collection.getId());
            statement.setString(2, collection.getName());
            statement.setString(3, collection.getGroupId());

            int rowsAffected = statement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully created EuroCoinCollection id={} name='{}' groupId={}",
                        collection.getId(), collection.getName(), collection.getGroupId());
            } else {
                logger.warn("Failed to create EuroCoinCollection id={} name='{}' groupId={}",
                        collection.getId(), collection.getName(), collection.getGroupId());
            }

            return success;

        } catch (SQLException e) {
            logger.error("Error creating EuroCoinCollection id={}", collection.getId(), e);
            return false;
        }
    }

    @Override
    public Optional<EuroCoinCollection> read(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot read EuroCoinCollection - id is null or blank");
            return Optional.empty();
        }

        String sql = String.format("""
                SELECT collection_id, name, group_id
                FROM %s
                WHERE collection_id = ?
                """, tableName);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            try (ResultSet queryResult = statement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinCollectionFromResultSet(id, queryResult);
                } else {
                    logger.debug("EuroCoinCollection with id: {} not found", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error reading EuroCoinCollection with id: {}", id, e);
            return Optional.empty();
        }
    }

    private Optional<EuroCoinCollection> createEuroCoinCollectionFromResultSet(String id,
            ResultSet queryResult) {
        try {
            EuroCoinCollection readCollection = euroCoinCollectionFactory
                    .fromDataBaseEntry(queryResult);
            if (!validateEuroCoinCollection(readCollection)) {
                logger.error("not valid EuroCoinCollection in database (id={})", id);
                return Optional.empty();
            }
            logger.debug("Successfully read EuroCoinCollection (id={})", id);
            return Optional.of(readCollection);
        } catch (SQLException e) {
            logger.warn("Invalid data for EuroCoinCollection row: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean update(EuroCoinCollection collection) {
        if (!validateEuroCoinCollection(collection)) {
            logger.warn("Cannot update EuroCoinCollection - validation failed");
            return false;
        }

        String sql = String.format("""
                UPDATE %s
                SET name = ?, group_id = ?
                WHERE collection_id = ?
                """, tableName);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, collection.getName());
            statement.setString(2, collection.getGroupId());

            int rowsAffected = statement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully updated EuroCoinCollection with id: {} in collection: {}",
                        collection.getId(), collection.getGroupId());
            } else {
                logger.warn("Failed to update EuroCoinCollection with id: {} in collection: {}",
                        collection.getId(), collection.getGroupId());
            }

            return success;

        } catch (SQLException e) {
            logger.error("Error updating EuroCoinCollection with id: {}", collection.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot delete EuroCoinCollection - id is null or blank");
            return false;
        }

        String sql = String.format("""
                DELETE FROM %s
                WHERE collection_id = ?
                """, tableName);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            int rowsAffected = statement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully deleted EuroCoinCollection with id: {}", id);
            } else {
                logger.warn("Failed to delete EuroCoinCollection with id: {}", id);
            }

            return success;

        } catch (SQLException e) {
            logger.warn("Error deleting EuroCoinCollection with id: {}", id, e);
            return false;
        }
    }

    @Override
    public List<EuroCoinCollection> getAll() {
        String sql = String.format("""
                SELECT collection_id, name, group_id
                FROM %s
                """, tableName);

        List<EuroCoinCollection> readCollections = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String collectionId = rs.getString("collection_id");
                Optional<EuroCoinCollection> readCollection = createEuroCoinCollectionFromResultSet(
                        collectionId, rs);
                if (readCollection.isPresent()) {
                    readCollections.add(readCollection.get());
                } else {
                    logger.warn(
                            "Skipping EuroCoinCollection row (collection_id={}) – invalid or incomplete data (validation failed)",
                            collectionId);
                }
            }
            logger.debug("Successfully retrieved {} EuroCoins from database",
                    readCollections.size());
        } catch (SQLException e) {
            logger.error("Error retrieving all EuroCoins from database", e);
        }

        return readCollections;
    }

    @Override
    public Optional<Boolean> exists(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot check existence of EuroCoinCollection - id is null or blank");
            return Optional.of(false);
        }

        String sql = String.format("""
                SELECT 1
                FROM %s
                WHERE collection_id = ?
                """, tableName);

        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return Optional.of(rs.next());
            }

        } catch (SQLException e) {
            logger.error("Error checking existence of EuroCoinCollection with id: {}", id, e);
            return Optional.empty();
        }
    }

    private boolean validateEuroCoinCollection(EuroCoinCollection collection) {
        if (collection == null) {
            logger.warn("Validation failed: EuroCoinCollection is null");
            return false;
        }

        if (collection.getId() == null || collection.getId().isBlank()) {
            logger.warn("Validation failed: EuroCoinCollection ID is null or empty");
            return false;
        }

        if (collection.getCoins() == null) {
            logger.warn("Validation failed: EuroCoinCollection coins (Arraylist) is null");
            return false;
        }

        logger.debug("Validation successful for EuroCoinCollection {}", collection.getId());
        return true;
    }
}
