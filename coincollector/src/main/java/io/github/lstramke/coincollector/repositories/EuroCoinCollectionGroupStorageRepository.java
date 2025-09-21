package io.github.lstramke.coincollector.repositories;

import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;

public interface EuroCoinCollectionGroupStorageRepository {
    boolean create(EuroCoinCollectionGroup group);
    Optional<EuroCoinCollectionGroup> read(String id);
    boolean update(EuroCoinCollectionGroup group);
    boolean delete(String id);
    List<EuroCoinCollectionGroup> getAllByUser(String userId);
    Optional<Boolean> exists(String id);
}
