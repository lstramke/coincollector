package io.github.lstramke.coincollector.repositories;

import java.util.List;
import java.util.Optional;

import io.github.lstramke.coincollector.model.EuroCoin;

public interface EuroCoinStorageRepository {
    boolean create(EuroCoin coin);
    Optional<EuroCoin> read(String id);
    boolean update(EuroCoin coin);
    boolean delete(String id);
    List<EuroCoin> getAll();
    Optional<Boolean> exists(String id);
}
