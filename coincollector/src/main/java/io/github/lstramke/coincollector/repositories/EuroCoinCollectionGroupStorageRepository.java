package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;

public interface EuroCoinCollectionGroupStorageRepository {
    void create(Connection connection, EuroCoinCollectionGroup group) throws SQLException;
    Optional<EuroCoinCollectionGroup> read(Connection connection, String id) throws SQLException;
    void update(Connection connection, EuroCoinCollectionGroup group) throws SQLException;
    void delete(Connection connection, String id) throws SQLException;
    List<EuroCoinCollectionGroup> getAllByUser(Connection connection, String userId) throws SQLException;
    boolean exists(Connection connection, String id) throws SQLException;
}
