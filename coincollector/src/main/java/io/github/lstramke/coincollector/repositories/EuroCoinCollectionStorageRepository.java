package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoinCollection;

/**
 * Repository abstraction for persisting and retrieving {@link EuroCoinCollection} entities using a
 * relational database. The current design is explicitly tailored to relational backends
 * because every operation requires an externally managed {@link java.sql.Connection}.
 * <p>
 * <strong>Lifecycle &amp; Transaction Management:</strong> Implementations MUST NOT open, commit, rollback
 * or close the provided {@link Connection}. That responsibility lies solely in the
 * service (application) layer which orchestrates transactions across multiple repository
 * calls. Each method is therefore expected to be side-effect free regarding the
 * connection lifecycle (no closing / committing / rolling back).
 * <p>
 * <strong>Scope:</strong> This repository is responsible ONLY for the persistence of the collection's own
 * attributes (identifier, name, grouping / ownership references, etc.). It deliberately does
 * NOT create, update, delete or otherwise manage the individual {@code EuroCoin} entries that
 * belong to a collection. Coin membership / coin persistence is handled by the dedicated
 * coin-related repositories. Callers must orchestrate multi-entity changes (e.g. creating a
 * collection and its coins) at a higher service layer, typically within a single transaction.
 */
public interface EuroCoinCollectionStorageRepository {

    /**
     * Persists a new {@link EuroCoinCollection}.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param collection aggregate to create; must not be {@code null} and must have a non-null / non-blank id & group id
     * @throws SQLException if a database access error occurs or the insert affects an unexpected number of rows
    * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code collection} violates validation constraints
     */
    void create(Connection connection, EuroCoinCollection collection) throws SQLException;

    /**
     * Reads a {@link EuroCoinCollection} by its collection id.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param collectionId identifier of the collection; must not be {@code null} or blank
     * @return optional containing the collection when found; otherwise empty
     * @throws SQLException if a database access error occurs
    * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code collectionId} is {@code null} or blank
     */
    Optional<EuroCoinCollection> read(Connection connection, String collectionId) throws SQLException;

    /**
     * Updates an existing {@link EuroCoinCollection}.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param collection updated aggregate; must not be {@code null} and must reference an existing collection id
     * @throws SQLException if a database access error occurs or the update affects an unexpected number of rows
    * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code collection} violates validation constraints
     */
    void update(Connection connection, EuroCoinCollection collection) throws SQLException;

    /**
     * Deletes a {@link EuroCoinCollection} by its identifier.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param collectionId identifier of the collection to delete; must not be {@code null} or blank
     * @throws SQLException if a database access error occurs
    * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code collectionId} is {@code null} or blank
     */
    void delete(Connection connection, String collectionId) throws SQLException;

    /**
     * Retrieves all persisted {@link EuroCoinCollection} rows.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @return list of collections (never {@code null}); may be empty
     * @throws SQLException if a database access error occurs
    * @throws IllegalArgumentException if {@code connection} is {@code null}
     */
    List<EuroCoinCollection> getAll(Connection connection) throws SQLException;

    /**
     * Checks if a {@link EuroCoinCollection} with the given id exists.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param collectionId identifier to check; must not be {@code null} or blank
     * @return {@code true} if a collection with the id exists; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code collectionId} is {@code null} or blank
     */
    boolean exists(Connection connection, String collectionId) throws SQLException;
}