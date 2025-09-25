package io.github.lstramke.coincollector.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EuroCoinCollectionGroupFactory {
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionGroupFactory.class);
    
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
