package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionUpdateException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupUpdateException;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionGroupStorageRepository;

/**
 * Thin service implementation of {@link EuroCoinCollectionGroupStorageService} that delegates to
 * {@link EuroCoinCollectionGroupStorageRepository} for group metadata and leverages
 * {@link EuroCoinCollectionStorageService} to populate groups with their collections. This class
 * manages connection/transaction boundaries itself for all public operations.
 */
public class EuroCoinCollectionGroupStorageServiceImpl implements EuroCoinCollectionGroupStorageService {

    private static final Logger logger = LoggerFactory.getLogger(EuroCoinCollectionGroupStorageServiceImpl.class);

    private final DataSource dataSource;
    private final EuroCoinCollectionGroupStorageRepository groupStorageRepository;
    private final EuroCoinCollectionStorageService euroCoinCollectionStorageService;

    public EuroCoinCollectionGroupStorageServiceImpl(DataSource dataSource, 
        EuroCoinCollectionGroupStorageRepository groupStorageRepository, 
        EuroCoinCollectionStorageService euroCoinCollectionStorageService) 
    {
        this.dataSource = dataSource;
        this.groupStorageRepository = groupStorageRepository;
        this.euroCoinCollectionStorageService = euroCoinCollectionStorageService;
    }

    /** {@inheritDoc} */
    @Override
    public void save(EuroCoinCollectionGroup group) throws EuroCoinCollectionGroupSaveException {
        if(group == null){
            logger.error("save() called with null group");
            throw new IllegalArgumentException();
        }
        logger.info("Saving group with id: {}", group.getId());
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                groupStorageRepository.create(connection, group);
                for (EuroCoinCollection collection : group.getCollections()) {
                    try {
                        euroCoinCollectionStorageService.save(collection, connection);
                    } catch (EuroCoinCollectionAlreadyExistsException e) {
                        euroCoinCollectionStorageService.updateMetadata(collection, connection);
                    }
                }
                connection.commit();
                logger.info("Group saved successfully: {}", group.getId());
            } catch (SQLException | EuroCoinCollectionSaveException | EuroCoinCollectionUpdateException e) {
                connection.rollback();
                logger.error("Error saving group {}: {}", group.getId(), e.getMessage(), e);
                throw new EuroCoinCollectionGroupSaveException(group.getId(), e);
            }
        } catch (SQLException e) {
            logger.error("SQL error saving group {}: {}", group.getId(), e.getMessage(), e);
            throw new EuroCoinCollectionGroupSaveException(group.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public EuroCoinCollectionGroup getById(String groupId) throws EuroCoinCollectionGroupGetByIdException, EuroCoinCollectionGroupNotFoundException {
        logger.info("Fetching group by id: {}", groupId);
        try (Connection connection = dataSource.getConnection()) {
            EuroCoinCollectionGroup group = groupStorageRepository
            .read(connection, groupId)
            .orElseThrow(() -> new EuroCoinCollectionGroupNotFoundException(groupId));
            euroCoinCollectionStorageService.getAll(connection).stream()
            .filter(collection -> Objects.equals(collection.getGroupId(), groupId))
            .forEach(group::addCollection);
            logger.info("Group fetched successfully: {}", groupId);
            return group;
        } catch (EuroCoinCollectionGetAllException e) {
            logger.error("Error fetching collections for group {}: {}", groupId, e.getMessage(), e);
            throw new EuroCoinCollectionGroupGetByIdException(groupId, e);
        } catch (SQLException e) {
            logger.error("SQL error fetching group {}: {}", groupId, e.getMessage(), e);
            throw new EuroCoinCollectionGroupGetByIdException(groupId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateMetadata(EuroCoinCollectionGroup group) throws EuroCoinCollectionGroupUpdateException {
        if(group == null){
            logger.error("updateMetadata() called with null group");
            throw new IllegalArgumentException();
        }
        logger.info("Updating group metadata for id: {}", group.getId());
        try (Connection connection = dataSource.getConnection()) {
            groupStorageRepository.update(connection, group);
            logger.info("Group metadata updated: {}", group.getId());
        } catch (SQLException e) {
            logger.error("SQL error updating group {}: {}", group.getId(), e.getMessage(), e);
            throw new EuroCoinCollectionGroupUpdateException(group.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String groupId) throws EuroCoinCollectionGroupDeleteException {
        logger.info("Deleting group with id: {}", groupId);
        try (Connection connection = dataSource.getConnection()) {
            groupStorageRepository.delete(connection, groupId);
            logger.info("Group deleted: {}", groupId);
        } catch (SQLException e) {
            logger.error("SQL error deleting group {}: {}", groupId, e.getMessage(), e);
            throw new EuroCoinCollectionGroupDeleteException(groupId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoinCollectionGroup> getAllByUser(String userId) throws EuroCoinCollectionGroupGetAllException {
        logger.info("Fetching all groups for user: {}", userId);
        try (Connection connection = dataSource.getConnection()) {
            Map<String, EuroCoinCollectionGroup> groupsByUser = groupStorageRepository
            .getAllByUser(connection, userId)
            .stream()
            .collect(Collectors.toMap(EuroCoinCollectionGroup::getId, c -> c));
        List<EuroCoinCollection> collections = euroCoinCollectionStorageService.getAll(connection);
        for(EuroCoinCollection collection : collections){
            if(groupsByUser.containsKey(collection.getGroupId())){
                groupsByUser.get(collection.getGroupId()).addCollection(collection);
            }
        }
        logger.info("Fetched {} groups for user {}", groupsByUser.size(), userId);
        return groupsByUser.values().stream().toList();
        } catch (SQLException | EuroCoinCollectionGetAllException e) {
           logger.error("Error fetching groups for user {}: {}", userId, e.getMessage(), e);
           throw new EuroCoinCollectionGroupGetAllException(e);
        }
    }
}
