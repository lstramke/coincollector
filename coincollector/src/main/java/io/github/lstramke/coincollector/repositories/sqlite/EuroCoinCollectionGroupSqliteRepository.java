package io.github.lstramke.coincollector.repositories.sqlite;

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

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroupFactory;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionGroupStorageRepository;

public class EuroCoinCollectionGroupSqliteRepository implements EuroCoinCollectionGroupStorageRepository {

    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionGroupSqliteRepository.class);
    private final String tableName;
    private final EuroCoinCollectionGroupFactory euroCoinCollectionGroupFactory;

    public EuroCoinCollectionGroupSqliteRepository(DataSource dataSource, String tableName, EuroCoinCollectionGroupFactory euroCoinCollectionGroupFactory) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.euroCoinCollectionGroupFactory = euroCoinCollectionGroupFactory;
    }

    @Override
    public boolean create(EuroCoinCollectionGroup group) {
        if(!validateEuroCoinCollectionGroup(group)){
            logger.warn("Cannot create EuroCoinCollectionGroup - validation failed");
            return false;
        }

        String sql = String.format(
            "INSERT INTO %s (group_id, name, owner_id) VALUES (?, ?, ?)", 
            tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, group.getId());
            preparedStatement.setString(2, group.getName());
            preparedStatement.setString(3, group.getOwnerId());

            int rowsAffected = preparedStatement.executeUpdate();
            boolean success = rowsAffected == 1;

            if(success){
                logger.info("Successfully created EuroCoinCollectionGroup id={}",
                        group.getId());
            } else {
                logger.warn("Failed to create EuroCoinCollectionGroup id={}",
                        group.getId());
            }
            
            return success;
        } catch (SQLException e) {
            logger.error("Error creating EuroCoinCollectionGroup id={}", group.getId(), e);
            return false;
        }
    }

    @Override
    public Optional<EuroCoinCollectionGroup> read(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot read EuroCoinCollectionGroup - id is null or blank");
            return Optional.empty();
        }

        String sql = String.format(
            """
            SELECT group_id, name, owner_id
            FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, id);
            try (ResultSet queryResult = preparedStatement.executeQuery()) {
                if (queryResult.next()) {
                    return createEuroCoinCollectionGroupFromResultSet(id, queryResult);
                } else {
                    logger.debug("EuroCoinCollectionGroup with id: {} not found", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error reading EuroCoinCollectionGroup with id: {}", id, e);
            return Optional.empty();
        }
    }

    private Optional<EuroCoinCollectionGroup> createEuroCoinCollectionGroupFromResultSet(String id, ResultSet queryResult) {
        try {
            EuroCoinCollectionGroup readCollection = euroCoinCollectionGroupFactory.fromDataBaseEntry(queryResult);
            logger.debug("Successfully read EuroCoinCollectionGroup (id={})", id);
            return Optional.of(readCollection);
        } catch (SQLException e) {
            logger.warn("Invalid data for EuroCoinCollectionGroup row: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean update(EuroCoinCollectionGroup group) {
       if (!validateEuroCoinCollectionGroup(group)) {
            logger.warn("Cannot update EuroCoinCollectionGroup - validation failed");
            return false;
        }

        String sql = String.format(
            """
            UPDATE %s
            SET name = ?, owner_id = ?
            WHERE group_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, group.getName());
            preparedStatement.setString(2, group.getOwnerId());
            preparedStatement.setString(3, group.getId());

            int rowsAffected = preparedStatement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully updated EuroCoinCollectionGroup with id: {}",
                        group.getId());
            } else {
                logger.warn("Failed to update EuroCoinCollectionGroup with id: {}",
                        group.getId());
            }

            return success;
        } catch (SQLException e) {
            logger.error("Error updating EuroCoinCollectionGroup with id: {}", group.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot delete EuroCoinCollectionGroup - id is null or blank");
            return false;
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, id);

            int rowsAffected = preparedStatement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully deleted EuroCoinCollectionGroup with id: {}", id);
            } else {
                logger.warn("Failed to delete EuroCoinCollectionGroup with id: {}", id);
            }

            return success;
        } catch (SQLException e) {
            logger.error("Error deleting EuroCoinCollectionGroup with id: {}", id, e);
            return false;
        }
    }

    @Override
    public List<EuroCoinCollectionGroup> getAllByUser(String userId) {
        List<EuroCoinCollectionGroup> readCollections = new ArrayList<>();

        if (userId == null || userId.isBlank()) {
            logger.warn("Cannot get EuroCoinCollectionGroups for userId - id is null or blank");
            return readCollections;
        }

        String sql = String.format(
            """
            SELECT group_id, name, owner_id
            FROM %s
            WHERE owner_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()){
                while (resultSet.next()) {
                    String groupId = resultSet.getString("group_id");
                    Optional<EuroCoinCollectionGroup> readCollection = createEuroCoinCollectionGroupFromResultSet(groupId, resultSet);
                    if (readCollection.isPresent()) {
                        readCollections.add(readCollection.get());
                    } else {
                        logger.warn("Skipping EuroCoinCollectionGroup row (group_id={}) – invalid or incomplete data", groupId);
                    }
                }
            }
            logger.debug("Successfully retrieved {} EuroCoinCollectionGroup(s) from database", readCollections.size());
        } catch (SQLException e) {
            logger.error("Error retrieving all EuroCoinCollectionGroup(s) from database for userId={}", userId, e);
        }

        return readCollections;
    }

    @Override
    public Optional<Boolean> exists(String id) {
        if (id == null || id.isBlank()) {
            logger.warn("Cannot check existence of EuroCoinCollectionGroup - id is null or blank");
            return Optional.of(false);
        }

        String sql = String.format(
            """
            SELECT 1
            FROM %s
            WHERE group_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return Optional.of(resultSet.next());
            }

        } catch (SQLException e) {
            logger.error("Error checking existence of EuroCoinCollectionGroup with id: {}", id, e);
            return Optional.empty();
        }
    }

    boolean validateEuroCoinCollectionGroup(EuroCoinCollectionGroup group){
        if(group == null){
            logger.warn("Validation failed: EuroCoinCollectionGroup is null");
            return false;
        }

        if(group.getId() == null || group.getId().isBlank()){
            logger.warn("Validation failed: EuroCoinCollectionGroup ID is null or empty");
            return false;
        }

        if(group.getOwnerId() == null || group.getOwnerId().isBlank()){
            logger.warn("Validation failed: EuroCoinCollectionGroup owner ID is null or empty");
            return false;
        }

        if(group.getCollections() == null){
            logger.warn("Validation failed: EuroCoinCollectionGroup collections (Arraylist) is null");
            return false;
        }

        logger.debug("Validation successful for EuroCoinCollectionGroup {}", group.getId());
        return true;
    }
}