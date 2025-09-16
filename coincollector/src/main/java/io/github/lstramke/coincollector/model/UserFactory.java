package io.github.lstramke.coincollector.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserFactory {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionFactory.class);

    public User fromDatabaseEntry(ResultSet resultSet) throws SQLException{
        try {
            String userId = resultSet.getString("user_id");
            String name = resultSet.getString("name");

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
