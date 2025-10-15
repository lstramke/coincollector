package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
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
        try (Connection connection = dataSource.getConnection()) {
                groupStorageRepository.create(connection, group);
        } catch (SQLException e) {
           throw new EuroCoinCollectionGroupSaveException(group.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public EuroCoinCollectionGroup getById(String groupId) throws EuroCoinCollectionGroupNotFoundException {
        try (Connection connection = dataSource.getConnection()) {
            EuroCoinCollectionGroup group = groupStorageRepository
            .read(connection, groupId)
            .orElseThrow(() -> new EuroCoinCollectionGroupNotFoundException(groupId));
            euroCoinCollectionStorageService.getAll(connection).stream()
            .filter(collection -> Objects.equals(collection.getGroupId(), groupId))
            .forEach(group::addCollection);
            return group;
        } catch (SQLException e) {
            throw new EuroCoinCollectionGroupNotFoundException(groupId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateMetadata(EuroCoinCollectionGroup group) throws EuroCoinCollectionGroupUpdateException {
        try (Connection connection = dataSource.getConnection()) {
            groupStorageRepository.update(connection, group);
        } catch (SQLException e) {
            throw new EuroCoinCollectionGroupUpdateException(group.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String groupId) throws EuroCoinCollectionGroupDeleteException {
        try (Connection connection = dataSource.getConnection()) {
            groupStorageRepository.delete(connection, groupId);
        } catch (SQLException e) {
            throw new EuroCoinCollectionGroupDeleteException(groupId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<EuroCoinCollectionGroup> getAllByUser(String userId) throws EuroCoinCollectionGroupGetAllException {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, EuroCoinCollectionGroup> groupsByUser = groupStorageRepository
            .getAllByUser(connection, userId)
            .stream()
            .collect(Collectors.toMap(EuroCoinCollectionGroup::getId, c -> c));
        List<EuroCoinCollection> collections = euroCoinCollectionStorageService.getAll(connection);
        for(EuroCoinCollection collection : collections){
            groupsByUser.get(collection.getGroupId()).addCollection(collection);
        }
        return groupsByUser.values().stream().toList();
        } catch (SQLException e) {
           throw new EuroCoinCollectionGroupGetAllException(e);
        }
    }
}
