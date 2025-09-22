package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoin;

public interface EuroCoinStorageRepository {
    void create(Connection connection, EuroCoin coin) throws SQLException;
    Optional<EuroCoin> read(Connection connection, String id) throws SQLException;
    void update(Connection connection, EuroCoin coin) throws SQLException;
    void delete(Connection connection, String id) throws SQLException;
    List<EuroCoin> getAll(Connection connection) throws SQLException;
    boolean exists(Connection connection, String id) throws SQLException;
}
