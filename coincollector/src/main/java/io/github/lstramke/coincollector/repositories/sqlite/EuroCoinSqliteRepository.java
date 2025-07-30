package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.repositories.EuroCoinStorageRepository;

public class EuroCoinSqliteRepository implements EuroCoinStorageRepository {

    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(EuroCoinSqliteRepository.class);
    private final String tableName;

    public EuroCoinSqliteRepository(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    @Override
    public boolean create(EuroCoin coin) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public EuroCoin read(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    @Override
    public boolean update(EuroCoin coin) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

    @Override
    public boolean delete(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public List<EuroCoin> getAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

    @Override
    public boolean exists(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exists'");
    }
}