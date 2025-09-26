package io.github.lstramke.coincollector.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link EuroCoinCollectionGroup} instances when the id is
 * already known.
 * <p>
 * Use this in scenarios where you must preserve existing identifiers (database
 * hydration, imports, synchronization, deterministic tests). The public
 * construction path generates a new random id, while the package-private
 * constructor used here keeps the provided one.
 * </p>
 */
public class EuroCoinCollectionGroupFactory {
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionGroupFactory.class);
    
    /**
     * Build a group from the current JDBC {@link ResultSet} row (DB hydration).
     * Preserves the existing id.
     * @param resultSet positioned result set
     * @return group instance with preserved id
     * @throws SQLException if invalid
     */
    public EuroCoinCollectionGroup fromDataBaseEntry(ResultSet resultSet) throws SQLException{
        try {
            String id = resultSet.getString("group_id");
            String name = resultSet.getString("name");
            String ownerId = resultSet.getString("owner_id");
            return new EuroCoinCollectionGroup(id, name, ownerId);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data in collectionGroup result set: {}", e.getMessage());
            throw new SQLException("Invalid collectionGroup database entry data", e);
        } catch (SQLException e) {
            logger.error("SQL error reading collectionGroup result set: {}", e.getMessage());
            throw e;
        }
    }
}
