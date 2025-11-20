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

/**
 * SQLite-backed implementation of {@link UserStorageRepository} providing simple CRUD
 * operations on a user table. Responsibilities:
 * <ul>
 *   <li>Create / read / update / delete of {@link User} rows</li>
 *   <li>Lightweight input validation (non-null, non-blank id)</li>
 *   <li>Entity mapping delegated to {@link UserFactory}</li>
 * </ul>
 * This class does NOT manage transactions or connection lifecycle; the caller must
 * supply an open {@link java.sql.Connection}. All JDBC resources (statements, result
 * sets) are closed via try-with-resources. Unexpected row counts during write
 * operations raise a {@link java.sql.SQLException}. 
 */

public class UserSqliteRepository implements UserStorageRepository{
    private static final Logger logger = LoggerFactory.getLogger(UserSqliteRepository.class);
    private final String tableName;
    private final UserFactory userFactory;

    public UserSqliteRepository(String tableName, UserFactory userFactory) {
        this.tableName = tableName;
        this.userFactory = userFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void create(Connection connection, User user) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (create)");
        }
        if(!validateUser(user)){
            logger.warn("User create aborted: validation failed");
            throw new IllegalArgumentException("User validation failed (create)");
        }

        String sql = String.format("INSERT INTO %s (user_id, username) VALUES (?, ?)", tableName);

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

    /** {@inheritDoc} */
    @Override
    public Optional<User> read(Connection connection, String userId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (read)");
        }
        if (userId == null || userId.isBlank()) {
            logger.warn("User read aborted: userId null/blank");
            throw new IllegalArgumentException("userId must not be null or blank (read)");
        }

        String sql = String.format(
            """
            SELECT user_id, username
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

    /**
     * Maps the current row of the given {@link ResultSet} to a {@link User} using the {@link UserFactory}.
     * <p>Contract:
     * <ul>
     *   <li>The {@code resultSet} must already be positioned on a valid row (i.e. after {@code next()} returned true).</li>
     *   <li>Returns an {@code Optional} that is always present unless a {@link SQLException} is thrown.</li>
     *   <li>Propagates any {@link SQLException} originating from the factory (invalid / incomplete row data).</li>
     * </ul>
     *
     * @param userIdentifierForLog id used only for logging correlation
     * @param resultSet JDBC result set positioned at the row to map
     * @return an {@link Optional} containing the mapped {@link User}
     * @throws SQLException if the mapping fails due to invalid column data or JDBC issues
     */
    private Optional<User> createUserFromResultSet(String userIdentifierForLog, ResultSet resultSet) throws SQLException{
        try {
            User readUser = userFactory.fromDataBaseEntry(resultSet);
            logger.debug("User read: id/username={}", userIdentifierForLog);
            return Optional.of(readUser);
        } catch (SQLException e) {
            logger.warn("User read produced invalid data: id/name={}", userIdentifierForLog);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(Connection connection, User user) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (update)");
        }
        if(!validateUser(user)){
            logger.warn("User update aborted: validation failed");
            throw new IllegalArgumentException("User validation failed (update)");
        }

        String sql = String.format(
            """
            UPDATE %s
            SET username = ?
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

    /** {@inheritDoc} */
    @Override
    public void delete(Connection connection, String userId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (delete)");
        }
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

    /** {@inheritDoc} */
    @Override
    public boolean exists(Connection connection, String userId) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (exists)");
        }
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

    /** {@inheritDoc} */
    @Override
    public Optional<User> getByUsername(Connection connection, String username) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null (getByUsername)");
        }
        if(username == null || username.isBlank()){
            logger.warn("Get by username aborted: username null/blank");
            throw new IllegalArgumentException("username must not be null or blank (getByUsername)");
        }

         String sql = String.format(
            """
            SELECT user_id, username
            FROM %s
            WHERE username = ?
            """, tableName
        );

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                if(resultSet.next()){
                    return createUserFromResultSet(username, resultSet);
                } else {
                    logger.debug("User not found: name={}", username);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("User read failed: name={}", username, e);
            throw e;
        }
    }

    /**
     * Internal (package-private) minimal validation of a {@link User} instance.
     * Current rules:
     * <ul>
     *   <li>User object must not be {@code null}</li>
     *   <li>Id must not be {@code null} or blank</li>
     *   <li>Name must not be {@code null} or blank </li>
     * </ul>
     * Extend here if additional invariants (e.g. name constraints) are required.
     *
     * @param user user instance to validate
     * @return {@code true} if all current rules are satisfied, otherwise {@code false}
     */
    boolean validateUser(User user){
        if (user == null) {
            return false;
        }

        String userId = user.getId();
        if (userId == null || userId.isBlank()) {
            return false;
        }

        String name = user.getName();
        if(name == null || name.isBlank()){
            return false;
        }

        return true;
    }
}
