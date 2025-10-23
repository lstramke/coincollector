package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionCoinsLoadException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionUpdateException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinUpdateException;
import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionStorageRepository;

/**
 * Thin service implementation of {@link EuroCoinCollectionStorageService} that
 * orchestrates persistence of {@link EuroCoinCollection} metadata via
 * {@link EuroCoinCollectionStorageRepository} and delegates coin persistence to
 * {@link EuroCoinStorageService}.
 *
 * Connection/transaction semantics:
 * - Methods without a {@link Connection} open a connection and manage
 *   transaction boundaries (commit/rollback) themselves.
 * - Methods with a {@link Connection} use a caller-managed connection and MUST
 *   NOT alter its lifecycle (no commit/rollback/close).
 *
 * Technical errors are translated to domain-specific exceptions where
 * applicable.
 */
public class EuroCoinCollectionStorageServiceImpl implements EuroCoinCollectionStorageService {
    
    private final EuroCoinCollectionStorageRepository euroCoinCollectionStorageRepository;
    private final DataSource dataSource;
    private final EuroCoinStorageService euroCoinStorageService;

    public EuroCoinCollectionStorageServiceImpl(
        EuroCoinCollectionStorageRepository euroCoinCollectionStorageRepository,
        DataSource dataSource, 
        EuroCoinStorageService euroCoinStorageService) {
            
        this.euroCoinCollectionStorageRepository = euroCoinCollectionStorageRepository;
        this.dataSource = dataSource;
        this.euroCoinStorageService = euroCoinStorageService;
    }

    /** {@inheritDoc} */
    @Override
    public void save(EuroCoinCollection euroCoinCollection) throws EuroCoinCollectionSaveException {
        try (Connection connection = dataSource.getConnection()) {
            try {
                connection.setAutoCommit(false);
                executeSave(euroCoinCollection, connection);
                connection.commit();
            } catch (SQLException | EuroCoinSaveException | EuroCoinUpdateException e) {
                connection.rollback();
                throw new EuroCoinCollectionSaveException(euroCoinCollection.getId(), e);
            }
        } catch (SQLException e) {
            throw new EuroCoinCollectionSaveException(euroCoinCollection.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void save(EuroCoinCollection euroCoinCollection, Connection connection) throws EuroCoinCollectionSaveException {
        try {
            executeSave(euroCoinCollection, connection);
        } catch (SQLException | EuroCoinSaveException | EuroCoinUpdateException e) {
            try {
                connection.rollback();
                throw new EuroCoinCollectionSaveException(euroCoinCollection.getId(), e);
            } catch (SQLException byRollback) {
                throw new EuroCoinCollectionSaveException(euroCoinCollection.getId(), e);
            }
        }
    }

    /**
     * Executes the save flow in one place to ensure identical behavior for both
     * public overloads. Persists the collection first, then persists or updates
     * all related coins. Transaction boundaries are handled by the caller.
     *
     * @param euroCoinCollection collection to persist
     * @param connection open JDBC connection managed by the caller
     * @throws SQLException if repository operations fail
     * @throws EuroCoinSaveException if saving a coin fails
     * @throws EuroCoinUpdateException if updating an existing coin fails
     */
    private void executeSave(EuroCoinCollection euroCoinCollection, Connection connection) throws SQLException, EuroCoinSaveException, EuroCoinUpdateException {
        euroCoinCollectionStorageRepository.create(connection, euroCoinCollection);
        for (EuroCoin euroCoin : euroCoinCollection.getCoins()) {
            try {
                euroCoinStorageService.save(euroCoin, connection);
            } catch (EuroCoinAlreadyExistsException e) {
                euroCoinStorageService.update(euroCoin, connection);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public EuroCoinCollection getById(String collectionId) throws EuroCoinCollectionNotFoundException, EuroCoinCollectionCoinsLoadException, EuroCoinCollectionGetByIdException {
        try (Connection connection = dataSource.getConnection()) {
            return executeGetById(collectionId, connection);
        } catch(EuroCoinGetAllException e) {
            throw new EuroCoinCollectionCoinsLoadException(collectionId, e);
        } catch (SQLException e) {
            throw new EuroCoinCollectionGetByIdException(collectionId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public EuroCoinCollection getById(String collectionId, Connection connection) throws EuroCoinCollectionNotFoundException, EuroCoinCollectionCoinsLoadException, EuroCoinCollectionGetByIdException {
        try {
            return executeGetById(collectionId, connection);
        } catch(EuroCoinGetAllException e) {
            throw new EuroCoinCollectionCoinsLoadException(collectionId);
        } catch (SQLException e) {
            throw new EuroCoinCollectionGetByIdException(collectionId, e);
        }
    }


    /**
     * Shared read flow for a collection: loads the collection metadata and
     * populates it with all related {@link EuroCoin} entries.
     *
     * @param collectionId id of the collection to load
     * @param connection open JDBC connection managed by the caller
     * @return the fully populated collection
     * @throws SQLException if repository access fails
     * @throws EuroCoinGetAllException if loading all coins fails
     * @throws EuroCoinCollectionNotFoundException if the collection does not exist
     */
    private EuroCoinCollection executeGetById(String collectionId, Connection connection) throws SQLException, EuroCoinGetAllException {
        EuroCoinCollection collection = euroCoinCollectionStorageRepository
            .read(connection, collectionId)
            .orElseThrow(() -> new EuroCoinCollectionNotFoundException(collectionId));
        euroCoinStorageService.getAll(connection).stream()
            .filter(coin -> Objects.equals(coin.getCollectionId(), collectionId))
            .forEach(collection::addCoin);
        return collection;
    }

    /** {@inheritDoc} */
    @Override
    public void updateMetadata(EuroCoinCollection euroCoinCollection) throws EuroCoinCollectionUpdateException {
        try (Connection connection = dataSource.getConnection()) {
            euroCoinCollectionStorageRepository.update(connection, euroCoinCollection);
        } catch (SQLException e) {
           throw new EuroCoinCollectionUpdateException(euroCoinCollection.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateMetadata(EuroCoinCollection euroCoinCollection, Connection connection) throws EuroCoinCollectionUpdateException {
        try {
            euroCoinCollectionStorageRepository.update(connection, euroCoinCollection);
        } catch (SQLException e) {
           throw new EuroCoinCollectionUpdateException(euroCoinCollection.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String collectionId) throws EuroCoinCollectionDeleteException {
        try (Connection connection = dataSource.getConnection()) {
            euroCoinCollectionStorageRepository.delete(connection, collectionId);
        } catch (SQLException e) {
           throw new EuroCoinCollectionDeleteException(collectionId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String collectionId, Connection connection) throws EuroCoinCollectionDeleteException {
        try {
            euroCoinCollectionStorageRepository.delete(connection, collectionId);
        } catch (SQLException e) {
           throw new EuroCoinCollectionDeleteException(collectionId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoinCollection> getAll() throws EuroCoinCollectionGetAllException {
        try (Connection connection = dataSource.getConnection()) {
            return executeGetAll(connection);
        } catch (SQLException | EuroCoinGetAllException e) {
            throw new EuroCoinCollectionGetAllException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoinCollection> getAll(Connection connection) throws EuroCoinCollectionGetAllException {
        try {
            return executeGetAll(connection);
        } catch (SQLException | EuroCoinGetAllException e) {
            throw new EuroCoinCollectionGetAllException(e);
        }
    }

    /**
     * Loads all collections and enriches them with their coins.
     *
     * Implementation details:
     * - Reads all {@link EuroCoinCollection} and indexes them by id.
     * - Reads all {@link EuroCoin} and attaches them to the appropriate collection.
     *
     * @param connection open JDBC connection managed by the caller
     * @return list of all collections including their coins
     * @throws SQLException if repository access fails
     */
    private List<EuroCoinCollection> executeGetAll(Connection connection) throws SQLException {
        Map<String, EuroCoinCollection> collectionMap = euroCoinCollectionStorageRepository
            .getAll(connection)
            .stream()
            .collect(Collectors.toMap(EuroCoinCollection::getId, c -> c));
        List<EuroCoin> coins = euroCoinStorageService.getAll(connection);
        for(EuroCoin coin : coins){
            collectionMap.get(coin.getCollectionId()).addCoin(coin);
        }
        return collectionMap.values().stream().toList();
    }
}
