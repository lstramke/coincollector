package io.github.lstramke.coincollector.services;

import java.util.List;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupUpdateException;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.EuroCoinCollection;

/**
 * Service abstraction for managing and retrieving {@link EuroCoinCollectionGroup}
 * entities using any relational database.
 * <p>
 * <strong>Lifecycle & transaction management:</strong>
 * This service does not expose {@code Connection}-based overloads. Implementations
 * must open/manage/close connections and transaction boundaries internally for each call.
 * <p>
 * <strong>Error/exception model:</strong>
 * Implementations should translate underlying technical errors into domain-specific exceptions:
 * <ul>
 *   <li>{@link EuroCoinCollectionGroupSaveException} for create/persist errors</li>
 *   <li>{@link EuroCoinCollectionGroupNotFoundException} when a group cannot be found</li>
 *   <li>{@link EuroCoinCollectionGroupUpdateException} for update errors</li>
 *   <li>{@link EuroCoinCollectionGroupDeleteException} for delete errors</li>
 *   <li>{@link EuroCoinCollectionGroupGetAllException} when retrieving all groups fails</li>
 * </ul>
 */
public interface EuroCoinCollectionGroupStorageService {
    /**
     * Persists a new {@link EuroCoinCollectionGroup} and manages connection/transaction boundaries internally.
     *
     * @param group group to persist; must not be {@code null} and must have valid attributes
     * @throws EuroCoinCollectionGroupSaveException if validation fails or persistence does not succeed
     */
    void save(EuroCoinCollectionGroup group) throws EuroCoinCollectionGroupSaveException;

    /**
     * Retrieves a {@link EuroCoinCollectionGroup} by its id and manages connection/transaction boundaries internally.
     *
     * @param groupId the group id to load; must not be {@code null} or blank
     * @return the found group
     * @throws EuroCoinCollectionGroupNotFoundException if no group with the given id exists
     */
    EuroCoinCollectionGroup getById(String groupId) throws EuroCoinCollectionGroupGetByIdException, EuroCoinCollectionGroupNotFoundException;

    /**
     * Updates an existing {@link EuroCoinCollectionGroup} and manages connection/transaction boundaries internally.
     *
     * @param group updated group; must not be {@code null} and must reference an existing id
     * @throws EuroCoinCollectionGroupUpdateException if validation fails or the update does not succeed
     */
    void updateMetadata(EuroCoinCollectionGroup group) throws EuroCoinCollectionGroupUpdateException;

    /**
     * Deletes a {@link EuroCoinCollectionGroup} by its id and manages connection/transaction boundaries internally.
     *
     * @param groupId the group id to delete; must not be {@code null} or blank
     * @throws EuroCoinCollectionGroupDeleteException if the delete operation fails
     */
    void delete(String groupId) throws EuroCoinCollectionGroupDeleteException;

    /**
     * Retrieves all {@link EuroCoinCollection} and manages connection/transaction boundaries internally.
     *
     * @return list of groups (possibly empty, never {@code null})
     * @throws EuroCoinCollectionGroupGetAllException when retrieving all groups fails
     */
    List<EuroCoinCollectionGroup> getAllByUser(String userId) throws EuroCoinCollectionGroupGetAllException;
}
