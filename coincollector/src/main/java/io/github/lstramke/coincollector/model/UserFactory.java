package io.github.lstramke.coincollector.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link User} instances when the id is already known.
 * <p>
 * Use this instead of the public {@code new User(String name)} constructor in
 * scenarios where you must preserve an existing identifier (e.g. database
 * hydration, importing from external systems, synchronization, tests with
 * stable ids). The public constructor always generates a new random id, while
 * the package-private {@code User(String id, String name)} constructor used
 * here preserves the supplied one.
 * </p>
 */
public class UserFactory {
    private static final Logger logger = LoggerFactory.getLogger(UserFactory.class);

    /**
     * Create a user from the current JDBC {@link ResultSet} row. Intended for
     * database hydration where the id column already exists.
     * @param resultSet positioned result set
     * @return user instance with preserved id
     * @throws SQLException if invalid data or SQL access failure occurs
     */
    public User fromDataBaseEntry(ResultSet resultSet) throws SQLException{
        try {
            String userId = resultSet.getString("user_id");
            String name = resultSet.getString("username");

            return new User(userId, name);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data in user result set: {}", e.getMessage());
            throw new SQLException("Invalid collection database entry data", e);
        } catch (SQLException e){
            logger.error("SQL error reading user result set: {}", e.getMessage());
            throw e;
        }
    }
}
