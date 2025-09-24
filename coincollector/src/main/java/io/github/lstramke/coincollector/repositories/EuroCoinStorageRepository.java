package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoin;

/**
 * Repository abstraction for persisting and retrieving {@link EuroCoin} entities using a
 * relational database. The current design is explicitly tailored to relational backends
 * because every operation requires an externally managed {@link java.sql.Connection}.
 * <p>
 * Lifecycle & transaction management: Implementations MUST NOT open, commit, rollback
 * or close the provided {@link Connection}. That responsibility lies solely in the
 * service (application) layer which orchestrates transactions across multiple repository
 * calls. Each method is therefore expected to be side-effect free regarding the
 * connection lifecycle (no closing / committing / rolling back).
 */
public interface EuroCoinStorageRepository {
    /**
     * Persists a new {@link EuroCoin}.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param coin coin instance to create; must not be {@code null} and must have a non-null / non-blank coinId
     * @throws SQLException if a database access error occurs or the insert fails
     * @throws IllegalArgumentException if {@code coin} violates validation constraints
     */
    void create(Connection connection, EuroCoin coin) throws SQLException;

    /**
     * Reads a {@link EuroCoin} by coinId.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param coinId identifier of the coin; must not be {@code null} or blank
     * @return optional containing the coin, or empty if not found
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code coinId} is {@code null} or blank
     */
    Optional<EuroCoin> read(Connection connection, String coinId) throws SQLException;

    /**
     * Updates an existing {@link EuroCoin}.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param coin updated coin; must not be {@code null} and must reference an existing coinId
     * @throws SQLException if a database access error occurs or the update affects an unexpected number of rows
     * @throws IllegalArgumentException if {@code coin} violates validation constraints
     */
    void update(Connection connection, EuroCoin coin) throws SQLException;

    /**
     * Deletes a {@link EuroCoin} by coinId.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param coinId identifier of the coin to delete; must not be {@code null} or blank
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code coinId} is {@code null} or blank
     */
    void delete(Connection connection, String coinId) throws SQLException;

    /**
     * Retrieves all {@link EuroCoin} rows.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @return list of coins (possibly empty, never {@code null})
     * @throws SQLException if a database access error occurs
     */
    List<EuroCoin> getAll(Connection connection) throws SQLException;

    /**
     * Checks existence of a {@link EuroCoin} by coinId.
     *
     * @param connection open JDBC connection; must not be {@code null}
     * @param coinId identifier to check; must not be {@code null} or blank
     * @return {@code true} if a coin with the coinId exists; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code coinId} is {@code null} or blank
     */
    boolean exists(Connection connection, String coinId) throws SQLException;
}
