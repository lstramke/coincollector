package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.util.List;

import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinUpdateException;
import io.github.lstramke.coincollector.model.EuroCoin;

/**
 * Service abstraction for managing and retrieving {@link EuroCoin} entities
 * using any relational database.
 * <p>
 * <strong>Lifecycle &amp; transaction management:</strong>
 * This interface exposes two variants for each operation:
 * <ul>
 *   <li>without {@link Connection}: The implementation is responsible for
 *       opening and managing a connection including transaction boundaries and
 *       closing it properly (commit/rollback). These methods encapsulate the
 *       technical details and are suitable when no explicit, cross-call
 *       transaction is required.</li>
 *   <li>with {@link Connection}: The connection is managed by the caller.
 *       Implementations MUST NOT open, commit, roll back or close the provided
 *       connection. This allows orchestrating a broader transaction across
 *       multiple repository/service calls.</li>
 * </ul>
 * <p>
 * <strong>Error/exception model:</strong>
 * Implementations should translate underlying technical errors into domain
 * specific exceptions:
 * <ul>
 *   <li>{@link EuroCoinSaveException} for create/persist errors</li>
 *   <li>{@link EuroCoinNotFoundException} when a coin cannot be found</li>
 *   <li>{@link EuroCoinUpdateException} for update errors</li>
 *   <li>{@link EuroCoinDeleteException} for delete errors</li>
 * </ul>
 * <p>
 */
public interface EuroCoinStorageService {
    /**
     * Persists a new {@link EuroCoin} and manages connection/transaction boundaries
     * internally.
     *
     * @param euroCoin coin to persist; must not be {@code null} and must have valid
     *                 domain id/attributes
     * @throws EuroCoinSaveException if validation fails or persistence does not succeed
     * @throws EuroCoinAlreadyExistsException if a coin with the same id already exists
     */
    void save(EuroCoin euroCoin) throws EuroCoinSaveException, EuroCoinAlreadyExistsException;

    /**
     * Persists a new {@link EuroCoin} using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param euroCoin coin to persist; must not be {@code null} and must have valid
     *                 domain id/attributes
     * @param connection open JDBC connection; must not be {@code null}
     * @throws EuroCoinSaveException if validation fails or persistence does not succeed
     * @throws EuroCoinAlreadyExistsException if a coin with the same id already exists
     */
    void save(EuroCoin euroCoin, Connection connection) throws EuroCoinSaveException, EuroCoinAlreadyExistsException;

    /**
     * Retrieves a {@link EuroCoin} by its id and manages connection/transaction
     * boundaries internally.
     *
     * @param coinId the coin id to load; must not be {@code null} or blank
     * @return the found coin
     * @throws EuroCoinNotFoundException if no coin with the given id exists
     */
    EuroCoin getById(String coinId) throws EuroCoinNotFoundException;

    /**
     * Retrieves a {@link EuroCoin} by its id using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param coinId the coin id to load; must not be {@code null} or blank
     * @param connection open JDBC connection; must not be {@code null}
     * @return the found coin
     * @throws EuroCoinNotFoundException if no coin with the given id exists
     */
    EuroCoin getById(String coinId, Connection connection) throws EuroCoinNotFoundException;

    /**
     * Updates an existing {@link EuroCoin} and manages connection/transaction
     * boundaries internally.
     *
     * @param euroCoin updated coin; must not be {@code null} and must reference an
     *                 existing id
     * @throws EuroCoinUpdateException if validation fails or the update does not succeed
     */
    void update(EuroCoin euroCoin) throws EuroCoinUpdateException;

    /**
     * Updates an existing {@link EuroCoin} using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param euroCoin updated coin; must not be {@code null} and must reference an
     *                 existing id
     * @param connection open JDBC connection; must not be {@code null}
     * @throws EuroCoinUpdateException if validation fails or the update does not succeed
     */
    void update(EuroCoin euroCoin, Connection connection) throws EuroCoinUpdateException;

    /**
     * Deletes a {@link EuroCoin} by its id and manages connection/transaction
     * boundaries internally.
     *
     * @param coinId the coin id to delete; must not be {@code null} or blank
     * @throws EuroCoinDeleteException if the delete operation fails
     */
    void delete(String coinId) throws EuroCoinDeleteException;

    /**
     * Deletes a {@link EuroCoin} by its id using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param coinId the coin id to delete; must not be {@code null} or blank
     * @param connection open JDBC connection; must not be {@code null}
     * @throws EuroCoinDeleteException if the delete operation fails
     */
    void delete(String coinId, Connection connection) throws EuroCoinDeleteException;

    /**
     * Retrieves all {@link EuroCoin} rows and manages connection/transaction
     * boundaries internally.
     *
     * @return list of coins (possibly empty, never {@code null})
     */
    List<EuroCoin> getAll();

    /**
     * Retrieves all {@link EuroCoin} rows using a caller-managed open JDBC
     * {@link Connection}. The implementation does not manage the connection lifecycle.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @return list of coins (possibly empty, never {@code null})
     */
    List<EuroCoin> getAll(Connection connection);
}
