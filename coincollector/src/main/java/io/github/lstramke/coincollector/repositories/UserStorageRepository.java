package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import io.github.lstramke.coincollector.model.User;

public interface UserStorageRepository {
    void create(Connection connection, User user) throws SQLException;
    Optional<User> read(Connection connection, String userId) throws SQLException;
    void update(Connection connection, User user) throws SQLException;
    void delete(Connection connection, String userId) throws SQLException;
    boolean exists(Connection connection, String id) throws SQLException;
}
