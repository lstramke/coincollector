package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;

/**
 * Repository abstraction for persisting and retrieving {@link EuroCoinCollectionGroup} entities using a
 * relational database. The current design is explicitly tailored to relational backends
 * because every operation requires an externally managed {@link java.sql.Connection}.
 * <p>
 * <strong>Lifecycle &amp; Transaction Management:</strong> Implementations MUST NOT open, commit, rollback
 * or close the provided {@link Connection}. That responsibility lies solely in the service (application)
 * layer which may orchestrate transactions across multiple repository calls. Each method MUST remain
 * side‑effect free regarding the connection lifecycle (no closing / committing / rolling back).
 * <p>
 * <strong>Scope:</strong> This repository is responsible ONLY for the persistence of a group's own
 * attributes (identifier, name, ownership reference). It deliberately does NOT create, update, delete
 * or otherwise manage the {@code EuroCoinCollection} membership belonging to a group. Collection
 * membership / collection persistence is handled by the dedicated collection-related repositories.
 * Callers must orchestrate multi-entity changes (e.g. creating a group and its collections) at a higher
 * service layer, typically within a single transaction.
 */
public interface EuroCoinCollectionGroupStorageRepository {
    /**
     * Persists a new {@link EuroCoinCollectionGroup}.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param group aggregate to create; must not be {@code null} and must have a non-null / non-blank id & owner id
     * @throws SQLException if a database access error occurs or the insert affects an unexpected number of rows
     * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code group} violates validation constraints
     */
    void create(Connection connection, EuroCoinCollectionGroup group) throws SQLException;

    /**
     * Reads a {@link EuroCoinCollectionGroup} by its group id.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param groupId identifier of the group; must not be {@code null} or blank
     * @return optional containing the group when found; otherwise empty
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code groupId} is {@code null} or blank
     */
    Optional<EuroCoinCollectionGroup> read(Connection connection, String groupId) throws SQLException;

    /**
     * Updates an existing {@link EuroCoinCollectionGroup}.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param group updated aggregate; must not be {@code null} and must reference an existing group id
     * @throws SQLException if a database access error occurs or the update affects an unexpected number of rows
     * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code group} violates validation constraints
     */
    void update(Connection connection, EuroCoinCollectionGroup group) throws SQLException;

    /**
     * Deletes a {@link EuroCoinCollectionGroup} by its identifier.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param groupId identifier of the group to delete; must not be {@code null} or blank
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code groupId} is {@code null} or blank
     */
    void delete(Connection connection, String groupId) throws SQLException;

    /**
     * Retrieves all persisted {@link EuroCoinCollectionGroup} rows for a given user.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param userId owner identifier whose groups to fetch; must not be {@code null} or blank
     * @return list of groups (never {@code null}); may be empty
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code userId} is {@code null} or blank
     */
    List<EuroCoinCollectionGroup> getAllByUser(Connection connection, String userId) throws SQLException;

    /**
     * Checks if a {@link EuroCoinCollectionGroup} with the given id exists.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param groupId identifier to check; must not be {@code null} or blank
     * @return {@code true} if a group with the id exists; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code connection} is {@code null} or if {@code groupId} is {@code null} or blank
     */
    boolean exists(Connection connection, String groupId) throws SQLException;
}
