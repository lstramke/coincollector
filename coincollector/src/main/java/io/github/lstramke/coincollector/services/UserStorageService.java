package io.github.lstramke.coincollector.services;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserDeleteException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserUpdateException;
import io.github.lstramke.coincollector.model.User;

/**
 * Service abstraction for managing and retrieving {@link User} entities using any
 * relational database.
 * <p>
 * <strong>Lifecycle &amp; transaction management:</strong>
 * Unlike {@code EuroCoinStorageService}, this service does not expose
 * {@code Connection}-based overloads. Implementations therefore open/manage/close
 * connections and transaction boundaries internally for each call.
 * <p>
 * <strong>Error/exception model:</strong>
 * Implementations should translate underlying technical errors into domain
 * specific exceptions:
 * <ul>
 *   <li>{@link UserSaveException} for create/persist errors</li>
 *   <li>{@link UserNotFoundException} when a user cannot be found</li>
 *   <li>{@link UserUpdateException} for update errors</li>
 *   <li>{@link UserDeleteException} for delete errors</li>
 * </ul>
 * <p>
 */
public interface UserStorageService {
    /**
     * Persists a new {@link User} and manages connection/transaction boundaries internally.
     *
     * @param user user to persist; must not be {@code null} and must have valid attributes
     * @throws UserSaveException if validation fails or persistence does not succeed
     */
    void save(User user) throws UserSaveException;

    /**
     * Retrieves a {@link User} by its id and manages connection/transaction boundaries internally.
     *
     * @param userId the user id to load; must not be {@code null} or blank
     * @return the found user
     * @throws UserNotFoundException if no user with the given id exists
     */
    User getById(String userId) throws UserNotFoundException;

    /**
     * Retrieves a {@link User} by its unique username.
     *
     * @param username the username to load; must not be {@code null} or blank
     * @return the found user
     * @throws UserNotFoundException if no user with the given username exists
     */
    User getByUsername(String username) throws UserNotFoundException;

    /**
     * Updates an existing {@link User} and manages connection/transaction boundaries internally.
     *
     * @param user updated user; must not be {@code null} and must reference an existing id
     * @throws UserUpdateException if validation fails or the update does not succeed
     */
    void update(User user) throws UserUpdateException;

    /**
     * Deletes a {@link User} by its id and manages connection/transaction boundaries internally.
     *
     * @param userId the user id to delete; must not be {@code null} or blank
     * @throws UserDeleteException if the delete operation fails
     */
    void delete(String userId) throws UserDeleteException;
}
