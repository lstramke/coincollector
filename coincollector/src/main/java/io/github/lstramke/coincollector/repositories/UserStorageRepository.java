package io.github.lstramke.coincollector.repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import io.github.lstramke.coincollector.model.User;

/**
 * Repository abstraction for persisting and retrieving {@link User} entities using a
 * relational database. The current design is explicitly tailored to relational backends
 * because every operation requires an externally managed {@link java.sql.Connection}.
 * <p>
 * <strong>Lifecycle &amp; Transaction Management:</strong> Implementations MUST NOT open, commit,
 * rollback or close the provided {@link Connection}. That responsibility lies solely in the
 * service (application) layer which orchestrates transactions across multiple repository
 * calls. Each method is therefore expected to be side-effect free regarding the
 * connection lifecycle (no closing / committing / rolling back).
 * <p>
 * <strong>Scope:</strong> This repository manages only the persistence of the {@code User}'s
 * intrinsic attributes (identifier, authentication / profile related fields, ownership
 * references, etc.). It does not itself cascade into or orchestrate persistence of related
 * domain concepts (e.g. coins, collections). Such multi-entity operations must be handled
 * in a coordinating service layer within a single transaction when atomicity is required.
 */
public interface UserStorageRepository {

    /**
     * Persists a new {@link User}.
     *
     * @param connection an open JDBC connection; must not be {@code null}
     * @param user the user instance to create; must not be {@code null} and must have a non-null and not-blank id
     * @throws SQLException if a database access error occurs or the insert fails
     * @throws IllegalArgumentException if {@code user} does not meet the required validation criteria
     */
    void create(Connection connection, User user) throws SQLException;

    /**
     * Reads a {@link User} by its identifier.
     *
     * @param connection an open JDBC connection; must not be {@code null}
     * @param userId the identifier of the user to fetch; must not be {@code null} and not blank
     * @return an {@link Optional} containing the found user, or empty if no user exists with the given id
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code userId} is {@code null} or blank
     */
    Optional<User> read(Connection connection, String userId) throws SQLException;

    /**
     * Updates an existing {@link User} record.
     *
     * @param connection an open JDBC connection; must not be {@code null}
     * @param user the user instance with updated state; must not be {@code null} and must have an existing, non-blank id
     * @throws SQLException if a database access error occurs or the update affects an unexpected number of rows
     * @throws IllegalArgumentException if {@code user} does not meet the required validation criteria
     */
    void update(Connection connection, User user) throws SQLException;

    /**
     * Deletes a {@link User} by its identifier.
     *
     * @param connection an open JDBC connection; must not be {@code null}
     * @param userId the identifier of the user to delete; must not be {@code null} and not blank
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code userId} is {@code null} or blank
     */
    void delete(Connection connection, String userId) throws SQLException;

    /**
     * Checks whether a {@link User} with the given id exists.
     *
     * @param connection an open JDBC connection; must not be {@code null}
     * @param userId the identifier to check; must not be {@code null} and not blank
     * @return {@code true} if a user with the given id exists; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if {@code userId} is {@code null} or blank
     */
    boolean exists(Connection connection, String userId) throws SQLException;

    /**
    * Reads a {@link User} by its username.
    *
    * @param connection an open JDBC connection; must not be {@code null}
    * @param username the username of the user to fetch; must not be {@code null} and not blank
    * @return an {@link Optional} containing the found user, or empty if no user exists with the given username
    * @throws SQLException if a database access error occurs
    * @throws IllegalArgumentException if {@code username} is {@code null} or blank
    */
    Optional<User> getByUsername(Connection connection, String username) throws SQLException;
}
