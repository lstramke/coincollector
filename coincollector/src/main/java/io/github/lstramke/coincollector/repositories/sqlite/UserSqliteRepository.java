package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.UserFactory;
import io.github.lstramke.coincollector.repositories.UserStorageRepository;

public class UserSqliteRepository implements UserStorageRepository{
    private static final Logger logger = LoggerFactory.getLogger(UserSqliteRepository.class);
    private final String tableName;
    private final UserFactory userFactory;

    public UserSqliteRepository(String tableName, UserFactory userFactory) {
        this.tableName = tableName;
        this.userFactory = userFactory;
    }

    @Override
    public void create(Connection connection, User user) throws SQLException {
        if(!validateUser(user)){
            logger.warn("User create aborted: validation failed");
            throw new IllegalArgumentException("User validation failed (create)");
        }

        String sql = String.format("INSERT INTO %s (user_id, name) VALUES (?, ?)", tableName);

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getId());
            preparedStatement.setString(2, user.getName());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 1) {
                logger.info("User created: id={}", user.getId());
            } else {
                logger.warn("User not created (rowsAffected={}): id={}", rowsAffected, user.getId());
                throw new SQLException("User create affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
           logger.error("User create failed: id={}", user.getId(), e);
           throw e;
        }
    }

    @Override
    public Optional<User> read(Connection connection, String userId) throws SQLException {
        if(userId == null || userId.isBlank()){
            logger.warn("User read aborted: userId null/blank");
            return Optional.empty();
        }

        String sql = String.format(
            """
            SELECT user_id, name
            FROM %s
            WHERE user_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                if(resultSet.next()){
                    return createUserFromResultSet(userId, resultSet);
                } else {
                    logger.debug("User not found: id={}", userId);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("User read failed: id={}", userId, e);
            throw e;
        }
    }

    private Optional<User> createUserFromResultSet(String userId, ResultSet resultSet){
        try {
            User readUser = userFactory.fromDataBaseEntry(resultSet);
            logger.debug("User read: id={}", userId);
            return Optional.of(readUser);
        } catch (SQLException e) {
            logger.warn("User read produced invalid data: id={}", userId);
            return Optional.empty();
        }
    }

    @Override
    public void update(Connection connection, User user) throws SQLException {
        if(!validateUser(user)){
            logger.warn("User update aborted: validation failed");
            throw new IllegalArgumentException("User validation failed (update)");
        }

        String sql = String.format(
            """
            UPDATE %s
            SET name = ?
            WHERE user_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.getId());

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 1) {
                logger.info("User updated: id={}", user.getId());
            } else {
                logger.warn("User not updated (rowsAffected={}): id={}", rowsAffected, user.getId());
                throw new SQLException("User update affected unexpected number of rows: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("User update failed: id={}", user.getId(), e);
            throw e;
        }
    }

    @Override
    public void delete(Connection connection, String userId) throws SQLException {
        if(userId == null || userId.isBlank()){
            logger.warn("User delete aborted: userId null/blank");
            throw new IllegalArgumentException("userId must not be null or blank (delete)");
        }

        String sql = String.format(
            """
            DELETE FROM %s
            WHERE user_id = ?
            """,tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);

            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected == 1) {
                logger.info("User deleted: id={}", userId);
            } else {
                logger.warn("User not deleted (rowsAffected={}): id={}", rowsAffected, userId);
                throw new SQLException("User delete affected unexpected number of rows: " + rowsAffected);
            }

        } catch (SQLException e) {
            logger.error("User delete failed: id={}", userId, e);
            throw e;
        }
    }

    @Override
    public boolean exists(Connection connection, String userId) throws SQLException {
        if(userId == null || userId.isBlank()){
            logger.warn("User exists check aborted: userId null/blank");
            throw new IllegalArgumentException("userId must not be null or blank (exists)");
        }

        String sql = String.format(
            """
            SELECT 1
            FROM %s
            WHERE user_id = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, userId);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("User exists check failed: id={}", userId, e);
            throw e;
        }
    }

    boolean validateUser(User user){
        if (user == null) {
            return false;
        }

        String userId = user.getId();
        if (userId == null || userId.isBlank()) {
            return false;
        }

        return true;
    }
}
