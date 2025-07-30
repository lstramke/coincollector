package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionGroupStorageRepository;

public class EuroCoinCollectionGroupSqliteRepository implements EuroCoinCollectionGroupStorageRepository {

    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionGroupSqliteRepository.class);
    private final String tableName;

    public EuroCoinCollectionGroupSqliteRepository(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    @Override
    public boolean create(EuroCoinCollectionGroup group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public EuroCoinCollectionGroup read(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    @Override
    public boolean update(EuroCoinCollectionGroup group) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public boolean delete(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<EuroCoinCollectionGroup> getAllByUser(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllByUser'");
    }

    @Override
    public boolean exists(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exists'");
    }
}