package io.github.lstramke.coincollector.repositories;

import java.util.Optional;

import io.github.lstramke.coincollector.model.User;

public interface UserStorageRepository {
    boolean create(User user);
    Optional<User> read(String userId);
    boolean update(User user);
    boolean delete(String userId);
    Optional<Boolean> exists(String id);
}
