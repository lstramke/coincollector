package io.github.lstramke.coincollector.repositories;

import java.util.List;

import io.github.lstramke.coincollector.model.EuroCoinCollection;

public interface EuroCoinCollectionStorageRepository {
    boolean create(EuroCoinCollection collection);
    EuroCoinCollection read(String id);
    boolean update(EuroCoinCollection collection);
    boolean delete(String id);
    List<EuroCoinCollection> getAll();
    boolean exists(String id);
}