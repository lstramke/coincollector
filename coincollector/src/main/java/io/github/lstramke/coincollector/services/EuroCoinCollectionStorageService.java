package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.util.List;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionUpdateException;
import io.github.lstramke.coincollector.model.EuroCoinCollection;

/**
 * Service abstraction for managing and retrieving {@link EuroCoinCollection} entities
 * using any relational database.
 * <p>
 * <strong>Lifecycle &amp; transaction management:</strong>
 * This interface exposes two variants for each operation:
 * <ul>
 *   <li>without {@link Connection}: The implementation is responsible for opening and
 *       managing a connection including transaction boundaries and closing it properly
 *       (commit/rollback). These methods encapsulate the technical details and are
 *       suitable when no explicit, cross-call transaction is required.</li>
 *   <li>with {@link Connection}: The connection is managed by the caller. Implementations
 *       MUST NOT open, commit, roll back or close the provided connection. This allows
 *       orchestrating a broader transaction across multiple repository/service calls.</li>
 * </ul>
 * <p>
 * <strong>Error/exception model:</strong>
 * Implementations should translate underlying technical errors into domain specific
 * exceptions:
 * <ul>
 *   <li>{@link EuroCoinCollectionSaveException} for create/persist errors</li>
 *   <li>{@link EuroCoinCollectionAlreadyExistsException} when a collection id already exists</li>
 *   <li>{@link EuroCoinCollectionNotFoundException} when a collection cannot be found</li>
 *   <li>{@link EuroCoinCollectionUpdateException} for update errors</li>
 *   <li>{@link EuroCoinCollectionDeleteException} for delete errors</li>
 *   <li>{@link EuroCoinCollectionGetAllException} when retrieving all collections fails</li>
 * </ul>
 */
public interface EuroCoinCollectionStorageService {
    /**
     * Persists a new {@link EuroCoinCollection} and manages connection/transaction
     * boundaries internally.
     *
     * @param euroCoinCollection collection to persist; must not be {@code null} and must have valid attributes
     * @throws EuroCoinCollectionSaveException if validation fails or persistence does not succeed
     * @throws EuroCoinCollectionAlreadyExistsException if a collection with the same id already exists
     */
    void save(EuroCoinCollection euroCoinCollection) throws EuroCoinCollectionSaveException;

    /**
     * Persists a new {@link EuroCoinCollection} using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param euroCoinCollection collection to persist; must not be {@code null} and must have valid attributes
     * @param connection open JDBC connection; must not be {@code null}
     * @throws EuroCoinCollectionSaveException if validation fails or persistence does not succeed
     * @throws EuroCoinCollectionAlreadyExistsException if a collection with the same id already exists
     */
    void save(EuroCoinCollection euroCoinCollection, Connection connection) throws EuroCoinCollectionSaveException;

    /**
     * Retrieves a {@link EuroCoinCollection} by its id and manages connection/transaction
     * boundaries internally.
     *
     * @param collectionId the collection id to load; must not be {@code null} or blank
     * @return the found collection
     * @throws EuroCoinCollectionNotFoundException if no collection with the given id exists
     */
    EuroCoinCollection getById(String collectionId) throws EuroCoinCollectionNotFoundException;

    /**
     * Retrieves a {@link EuroCoinCollection} by its id using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param collectionId the collection id to load; must not be {@code null} or blank
     * @param connection open JDBC connection; must not be {@code null}
     * @return the found collection
     * @throws EuroCoinCollectionNotFoundException if no collection with the given id exists
     */
    EuroCoinCollection getById(String collectionId, Connection connection) throws EuroCoinCollectionNotFoundException;

    /**
     * Updates an existing {@link EuroCoinCollection} and manages connection/transaction
     * boundaries internally.
     *
     * @param euroCoinCollection updated collection; must not be {@code null} and must reference an existing id
     * @throws EuroCoinCollectionUpdateException if validation fails or the update does not succeed
     */
    void updateMetadata(EuroCoinCollection euroCoinCollection) throws EuroCoinCollectionUpdateException;

    /**
     * Updates an existing {@link EuroCoinCollection} using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param euroCoinCollection updated collection; must not be {@code null} and must reference an existing id
     * @param connection open JDBC connection; must not be {@code null}
     * @throws EuroCoinCollectionUpdateException if validation fails or the update does not succeed
     */
    void updateMetadata(EuroCoinCollection euroCoinCollection, Connection connection) throws EuroCoinCollectionUpdateException;

    /**
     * Deletes a {@link EuroCoinCollection} by its id and manages connection/transaction
     * boundaries internally.
     *
     * @param collectionId the collection id to delete; must not be {@code null} or blank
     * @throws EuroCoinCollectionDeleteException if the delete operation fails
     */
    void delete(String collectionId) throws EuroCoinCollectionDeleteException;

    /**
     * Deletes a {@link EuroCoinCollection} by its id using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param collectionId the collection id to delete; must not be {@code null} or blank
     * @param connection open JDBC connection; must not be {@code null}
     * @throws EuroCoinCollectionDeleteException if the delete operation fails
     */
    void delete(String collectionId, Connection connection) throws EuroCoinCollectionDeleteException;

    /**
     * Retrieves all {@link EuroCoinCollection} rows and manages connection/transaction
     * boundaries internally.
     *
     * @return list of collections (possibly empty, never {@code null})
     * @throws EuroCoinCollectionGetAllException when retrieving all collections fails
     */
    List<EuroCoinCollection> getAll() throws EuroCoinCollectionGetAllException;

    /**
     * Retrieves all {@link EuroCoinCollection} rows using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @return list of collections (possibly empty, never {@code null})
     * @throws EuroCoinCollectionGetAllException when retrieving all collections fails
     */
    List<EuroCoinCollection> getAll(Connection connection) throws EuroCoinCollectionGetAllException;
}
