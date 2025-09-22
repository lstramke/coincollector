package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoinCollection;

public interface EuroCoinCollectionStorageRepository {
    void create(Connection connection, EuroCoinCollection collection) throws SQLException;
    Optional<EuroCoinCollection> read(Connection connection, String id) throws SQLException;
    void update(Connection connection, EuroCoinCollection collection) throws SQLException;
    void delete(Connection connection, String id) throws SQLException;
    List<EuroCoinCollection> getAll(Connection connection) throws SQLException;
    boolean exists(Connection connection, String id) throws SQLException;
}