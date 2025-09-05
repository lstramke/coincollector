package io.github.lstramke.coincollector.repositories;

import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoinCollection;

public interface EuroCoinCollectionStorageRepository {
    boolean create(EuroCoinCollection collection);
    Optional<EuroCoinCollection> read(String id);
    boolean update(EuroCoinCollection collection);
    boolean delete(String id);
    List<EuroCoinCollection> getAll();
    Optional<Boolean> exists(String id);
}