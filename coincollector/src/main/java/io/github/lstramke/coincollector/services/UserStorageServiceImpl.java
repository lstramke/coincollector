package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserDeleteException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserUpdateException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.repositories.UserStorageRepository;

/**
 * Thin service implementation of {@link UserStorageService} that delegates to
 * {@link UserStorageRepository} and handles connection/transaction boundaries
 * when no {@link Connection} is supplied by the caller. 
 */
public class UserStorageServiceImpl implements UserStorageService {

    private final UserStorageRepository userStorageRepository;
    private final DataSource dataSource;

    public UserStorageServiceImpl(UserStorageRepository userStorageRepository, DataSource dataSource){
        this.userStorageRepository = userStorageRepository;
        this.dataSource = dataSource;
    }

    /** {@inheritDoc} */
    @Override
    public void save(User user) throws UserSaveException {
        try (Connection connection = dataSource.getConnection()) {
            userStorageRepository.create(connection, user);
        } catch (SQLException e) {
            throw new UserSaveException(user.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public User getById(String userId) throws UserNotFoundException {
        try (Connection connection = dataSource.getConnection()) {
            return userStorageRepository
                .read(connection, userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        } catch (SQLException e) {
            throw new UserNotFoundException(userId, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public User getByUsername(String username) throws UserNotFoundException {
        try (Connection connection = dataSource.getConnection()) {
            return userStorageRepository
                .getByUsername(connection, username)
                .orElseThrow(() -> new UserNotFoundException(username));
        } catch (SQLException e) {
            throw new UserNotFoundException(username, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(User user) throws UserUpdateException {
        try (Connection connection = dataSource.getConnection()) {
            userStorageRepository.update(connection, user);
        } catch (SQLException e) {
            throw new UserUpdateException(user.getId(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String userId) throws UserDeleteException {
        try (Connection connection = dataSource.getConnection()) {
            userStorageRepository.delete(connection, userId);
        } catch (SQLException e) {
            throw new UserDeleteException(userId, e);
        }
    }
}
