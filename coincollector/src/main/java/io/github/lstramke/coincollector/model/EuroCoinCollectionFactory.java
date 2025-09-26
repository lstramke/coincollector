package io.github.lstramke.coincollector.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating {@link EuroCoinCollection} instances when the id is
 * already known.
 * <p>
 * Use this in scenarios where existing identifiers must be preserved (database
 * hydration, imports, synchronization, deterministic tests). The public
 * constructors of {@link EuroCoinCollection} generate a new random id, while
 * the package-private constructor used here keeps the provided one.
 * </p>
 */
public class EuroCoinCollectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionFactory.class);

    /**
     * Build a collection from the current JDBC {@link ResultSet} row (DB
     * hydration). Preserves the existing id from the row.
     * @param resultSet positioned result set
     * @return collection instance with preserved id
     * @throws SQLException if data invalid or SQL access error
     */
    public EuroCoinCollection fromDataBaseEntry(ResultSet resultSet) throws SQLException {
        try {
            String id = resultSet.getString("collection_id");
            String name = resultSet.getString("name");
            String groupId = resultSet.getString("group_id");

            return new EuroCoinCollection(id, name, groupId);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data in collection result set: {}", e.getMessage());
            throw new SQLException("Invalid collection database entry data", e);
        } catch (SQLException e) {
            logger.error("SQL error reading collection result set: {}", e.getMessage());
            throw e;
        }
    }
}
