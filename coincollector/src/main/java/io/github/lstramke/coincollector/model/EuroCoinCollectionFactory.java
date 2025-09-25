package io.github.lstramke.coincollector.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EuroCoinCollectionFactory {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionFactory.class);

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
