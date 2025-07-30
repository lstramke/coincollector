package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionStorageRepository;

public class EuroCoinCollectionSqliteRepository implements EuroCoinCollectionStorageRepository {

    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionSqliteRepository.class);
    private final String tableName;

    public EuroCoinCollectionSqliteRepository(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    @Override
    public boolean create(EuroCoinCollection collection) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public EuroCoinCollection read(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    @Override
    public boolean update(EuroCoinCollection collection) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public boolean delete(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<EuroCoinCollection> getAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

    @Override
    public boolean exists(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exists'");
    }
}