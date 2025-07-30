package io.github.lstramke.coincollector.repositories;

import java.util.List;

import io.github.lstramke.coincollector.model.EuroCoin;

public interface EuroCoinStorageRepository {
    boolean create(EuroCoin coin);
    EuroCoin read(String id);
    boolean update(EuroCoin coin);
    boolean delete(String id);
    List<EuroCoin> getAll();
    boolean exists(String id);
}
