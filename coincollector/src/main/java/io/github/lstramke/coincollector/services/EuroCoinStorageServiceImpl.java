package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinUpdateException;
import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.repositories.EuroCoinStorageRepository;

/**
 * Thin service implementation of {@link EuroCoinStorageService} that delegates to
 * {@link EuroCoinStorageRepository} and handles connection/transaction boundaries
 * when no {@link Connection} is supplied by the caller. 
 */
public class EuroCoinStorageServiceImpl implements EuroCoinStorageService {

    private final EuroCoinStorageRepository euroCoinStorageRepository;
    private final DataSource dataSource;

    public EuroCoinStorageServiceImpl(EuroCoinStorageRepository euroCoinStorageRepository, DataSource dataSource){
        this.euroCoinStorageRepository = euroCoinStorageRepository;
        this.dataSource = dataSource;
    }

    /** {@inheritDoc} */
    @Override
    public void save(EuroCoin euroCoin) throws EuroCoinSaveException, EuroCoinAlreadyExistsException {
        if(euroCoin == null){
            throw new IllegalArgumentException("euroCoin must not be null (save)");
        }

        try (Connection connection = dataSource.getConnection()) {
            executeSave(euroCoin, connection);
        } catch (SQLException e) {
            throw new EuroCoinSaveException(euroCoin.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void save(EuroCoin euroCoin, Connection connection) throws EuroCoinSaveException, EuroCoinAlreadyExistsException {
        if(euroCoin == null){
            throw new IllegalArgumentException("euroCoin must not be null (save)");
        }

        try {
            executeSave(euroCoin, connection);
        } catch (SQLException e) {
            throw new EuroCoinSaveException(euroCoin.getId(), e);
        }
    }

    /**
     * Executes the save flow in one place to ensure identical behavior for both
     * public overloads. Centralizes the existence check to provide a clear
     * Already-Exists signal and keeps transactional semantics consistent.
     *
     * @param euroCoin coin to persist; must not be null
     * @param connection open JDBC connection managed by the caller
     * @throws SQLException if the existence check or insert fails
     * @throws EuroCoinAlreadyExistsException if a coin with the same id already exists
     */
    private void executeSave(EuroCoin euroCoin, Connection connection) throws SQLException, EuroCoinAlreadyExistsException {
        if(!euroCoinStorageRepository.exists(connection, euroCoin.getId())){
            euroCoinStorageRepository.create(connection, euroCoin);
        } else {
            throw new EuroCoinAlreadyExistsException(euroCoin.getId());
        }
    }

    /** {@inheritDoc} */
    @Override
    public EuroCoin getById(String coinId) throws EuroCoinNotFoundException {
        try (Connection connection = dataSource.getConnection()) {
            return executeGetById(coinId, connection);
        } catch (SQLException e) {
            throw new EuroCoinNotFoundException(coinId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public EuroCoin getById(String coinId, Connection connection) throws EuroCoinNotFoundException {
        try {
            return executeGetById(coinId, connection);
        } catch (SQLException e) {
            throw new EuroCoinNotFoundException(coinId, e);
        }
    }

    /**
     * Shared read flow to keep not-found handling and repository interaction
     * consistent across both overloads.
     *
     * @param coinId coin id to retrieve
     * @param connection open JDBC connection managed by the caller
     * @return the found coin
     * @throws SQLException if the read operation fails
     * @throws EuroCoinNotFoundException if the coin does not exist
     */
    private EuroCoin executeGetById(String coinId, Connection connection) throws SQLException {
        return euroCoinStorageRepository
            .read(connection, coinId)
            .orElseThrow(() -> new EuroCoinNotFoundException(coinId));
    }

    /** {@inheritDoc} */
    @Override
    public void update(EuroCoin euroCoin) throws EuroCoinUpdateException {
        try (Connection connection = dataSource.getConnection()) {
            euroCoinStorageRepository.update(connection, euroCoin);
        } catch (SQLException e) {
            throw new EuroCoinUpdateException(euroCoin.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(EuroCoin euroCoin, Connection connection) throws EuroCoinUpdateException {
        try {
            euroCoinStorageRepository.update(connection, euroCoin);
        } catch (SQLException e) {
            throw new EuroCoinUpdateException(euroCoin.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String coinId) throws EuroCoinDeleteException {
        try (Connection connection = dataSource.getConnection()) {
            euroCoinStorageRepository.delete(connection, coinId);
        } catch (SQLException e) {
            throw new EuroCoinDeleteException(coinId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String coinId, Connection connection) throws EuroCoinDeleteException {
        try {
            euroCoinStorageRepository.delete(connection, coinId);
        } catch (SQLException e) {
            throw new EuroCoinDeleteException(coinId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoin> getAll() {
        try (Connection connection = dataSource.getConnection()) {
            return euroCoinStorageRepository.getAll(connection);
        } catch (SQLException e) {
            throw new EuroCoinGetAllException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoin> getAll(Connection connection) {
        try {
            return euroCoinStorageRepository.getAll(connection);
        } catch (SQLException e) {
            throw new EuroCoinGetAllException(e);
        }
    }
}
