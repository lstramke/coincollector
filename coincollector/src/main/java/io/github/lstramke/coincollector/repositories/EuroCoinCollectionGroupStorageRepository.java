package io.github.lstramke.coincollector.repositories;

import java.util.List;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;

public interface EuroCoinCollectionGroupStorageRepository {
    boolean create(EuroCoinCollectionGroup group);
    EuroCoinCollectionGroup read(String id);
    boolean update(EuroCoinCollectionGroup group);
    boolean delete(String id);
    List<EuroCoinCollectionGroup> getAllByUser(String userId);
    boolean exists(String id);
}
