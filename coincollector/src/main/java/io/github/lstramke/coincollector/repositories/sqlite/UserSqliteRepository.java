package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.UserFactory;
import io.github.lstramke.coincollector.repositories.UserStorageRepository;

public class UserSqliteRepository implements UserStorageRepository{
    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(UserSqliteRepository.class);
    private final String tableName;
    private final UserFactory userFactory;

    public UserSqliteRepository(DataSource dataSource, String tableName, UserFactory userFactory) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        this.userFactory = userFactory;
    }

    @Override
    public boolean create(User user) {
        if(!validateUser(user)){
            logger.warn("Cannot create User - validation failed");
            return false;
        }

        String sql = String.format("INSERT INTO %s (user_id, name) VALUES (?, ?)", tableName);

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, user.getId());
            preparedStatement.setString(2, user.getName());

            int rowsAffected = preparedStatement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully created User with userId: {}", user.getId());
            } else {
                logger.warn("Failed to create User with userId: {}", user.getId());
            }

            return success;
        } catch (SQLException e) {
           logger.error("Error creating User with userId: {}", user.getId(), e);
           return false;
        }
    }

    @Override
    public Optional<User> read(String userId) {
        if(userId == null || userId.isBlank()){
            logger.warn("Cannot read User - userId is null or blank");
            return Optional.empty();
        }

        String sql = String.format(
            """
            SELECT user_id, name
            FROM %s
            WHERE user_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, userId);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                if(resultSet.next()){
                    return createUserFromResultSet(userId, resultSet);
                } else {
                    logger.debug("User with userId: {} not found", userId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error reading User with userId: {}", userId, e);
            return Optional.empty();
        }
    }

    private Optional<User> createUserFromResultSet(String userId, ResultSet resultSet){
        try {
            User readUser = userFactory.fromDataBaseEntry(resultSet);
            logger.debug("Successfully read User with userId: {}", userId);
            return Optional.of(readUser);
        } catch (SQLException e) {
            logger.warn("Invalid data for User with userId: {} - {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean update(User user) {
        if(!validateUser(user)){
            logger.warn("Cannot update User - validation failed");
            return false;
        }

        String sql = String.format(
            """
            UPDATE %s
            SET name = ?
            WHERE user_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getId());

            int rowsAffected = preparedStatement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully updated User with userId: {}", user.getId());
            } else {
                logger.warn("Failed to update User with userId: {}", user.getId());
            }

            return success;
        } catch (SQLException e) {
            logger.error("Error updating User with userId: {}", user.getId(), e);
            return false;
        }
    }

    @Override
    public boolean delete(String userId) {
        if(userId == null || userId.isBlank()){
            logger.warn("Cannot delete User - userId is null or blank");
            return false;
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE user_id = ?
            """,tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, userId);

            int rowsAffected = preparedStatement.executeUpdate();
            boolean success = rowsAffected == 1;

            if (success) {
                logger.info("Successfully deleted User with userId: {}", userId);
            } else {
                logger.warn("Failed to delete User with userId: {}", userId);
            }

            return success;
        } catch (SQLException e) {
            logger.error("Error deleting User with userId: {}", userId, e);
            return false;
        }
    }

    @Override
    public Optional<Boolean>  exists(String userId) {
        if(userId == null || userId.isBlank()){
            logger.warn("Cannot check existence - userId is null or blank");
            return Optional.of(false);
        }

        String sql = String.format(
            """
            SELECT 1
            FROM %s
            WHERE user_id = ?
            """, tableName
        );

        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, userId);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                return Optional.of(rs.next());
            }
        } catch (SQLException e) {
            logger.error("Error checking existence of User with userId: {}", userId, e);
            return Optional.empty();
        }
    }

    boolean validateUser(User user){
        if (user == null) {
            logger.warn("Cannot validate User - user is null");
            return false;
        }

        String userId = user.getId();
        if (userId == null || userId.isBlank()) {
            logger.warn("Cannot validate User - userId is null or blank");
            return false;
        }

        return true;
    }
}
