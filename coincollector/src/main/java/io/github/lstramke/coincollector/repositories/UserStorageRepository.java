package io.github.lstramke.coincollector.repositories;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.User;

public interface UserStorageRepository {
    boolean create(User user);
    EuroCoinCollectionGroup read(String userId);
    boolean update(User user);
    boolean delete(String userId);
    boolean exists(String id);
}
