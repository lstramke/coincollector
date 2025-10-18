package io.github.lstramke.coincollector.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserDeleteException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;
import io.github.lstramke.coincollector.exceptions.userExceptions.UserUpdateException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.repositories.UserStorageRepository;

@ExtendWith(MockitoExtension.class)
public class UserStorageServiceImplTest {

    private static final User dummyUser = new User("Bob");

    private record SaveTestcase(
        User user,
        boolean getConnectionThrows,
        boolean repoThrowsSQLException,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<SaveTestcase> saveTestcases(){
        return Stream.of(
            new SaveTestcase(dummyUser, false, false, null, "Valid user - saved successfully"),
            new SaveTestcase(dummyUser, true, false, UserSaveException.class, "DataSource throws SQLException"),
            new SaveTestcase(dummyUser, false, true, UserSaveException.class, "Repository throws SQLException")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("saveTestcases")
    void testSave(SaveTestcase testcase) {
        DataSource dataSource = mock(DataSource.class);
        UserStorageRepository repository = mock(UserStorageRepository.class);
        UserStorageServiceImpl service = new UserStorageServiceImpl(repository, dataSource);

        Connection connection = mock(Connection.class);

        try {   
            if (testcase.getConnectionThrows) {
                when(dataSource.getConnection()).thenThrow(new SQLException("DS error"));
            } else {
                when(dataSource.getConnection()).thenReturn(connection);
                if (testcase.user != null) {
                    if (testcase.repoThrowsSQLException) {
                        doThrow(new SQLException("Repo create error")).when(repository).create(connection, testcase.user);
                    } else {
                        doNothing().when(repository).create(connection, testcase.user);
                    }
                } else {
                    doThrow(new IllegalArgumentException("user must not be null")).when(repository).create(eq(connection), isNull());
                }
            }
    
            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.save(testcase.user));
            } else {
                assertDoesNotThrow(() -> service.save(testcase.user));
                verify(dataSource).getConnection();
                verify(repository).create(connection, testcase.user);
            }
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetByIdTestcase(
        String id,
        boolean getConnectionThrows,
        boolean repoThrowsSQLException,
        boolean repoReturnsEmpty,
        Optional<User> repoResult,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetByIdTestcase> getByIdTestcases(){
        return Stream.of(
            new GetByIdTestcase("validId", false, false, false, Optional.of(dummyUser), null, "Valid id - user found"),
            new GetByIdTestcase("validId", false, false, true, Optional.empty(), UserNotFoundException.class, "Valid id - user not found"),
            new GetByIdTestcase("validId", false, true, false, Optional.empty(), UserNotFoundException.class, "Repository throws SQLException"),
            new GetByIdTestcase("validId", true, false, false, Optional.empty(), UserNotFoundException.class, "DataSource throws SQLException")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getByIdTestcases")
    void testGetById(GetByIdTestcase testcase) {
        DataSource dataSource = mock(DataSource.class);
        UserStorageRepository repository = mock(UserStorageRepository.class);
        UserStorageServiceImpl service = new UserStorageServiceImpl(repository, dataSource);
        Connection connection = mock(Connection.class);

        try {
            if (testcase.getConnectionThrows) {
                when(dataSource.getConnection()).thenThrow(new SQLException("DS error"));
            } else {
                when(dataSource.getConnection()).thenReturn(connection);
    
                if (testcase.repoThrowsSQLException) {
                    when(repository.read(connection, testcase.id)).thenThrow(new SQLException("Repo read error"));
                } else if (testcase.repoReturnsEmpty) {
                    when(repository.read(connection, testcase.id)).thenReturn(Optional.empty());
                } else {
                    when(repository.read(connection, testcase.id)).thenReturn(testcase.repoResult);
                }
            }
    
            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.getById(testcase.id));
            } else {
                User result = assertDoesNotThrow(() -> service.getById(testcase.id));
                assertEquals(testcase.repoResult.get(), result);
                verify(dataSource).getConnection();
                verify(repository).read(connection, testcase.id);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetByUsernameTestcase(
        String username,
        boolean getConnectionThrows,
        boolean repoThrowsSQLException,
        boolean repoReturnsEmpty,
        Optional<User> repoResult,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetByUsernameTestcase> getByUsernameTestcases(){
        return Stream.of(
            new GetByUsernameTestcase("validName", false, false, false, Optional.of(dummyUser), null, "Valid name - user found"),
            new GetByUsernameTestcase("validName", false, false, true, Optional.empty(), UserNotFoundException.class, "Valid name - user not found"),
            new GetByUsernameTestcase("validName", false, true, false, Optional.empty(), UserNotFoundException.class, "Repository throws SQLException"),
            new GetByUsernameTestcase("validName", true, false, false, Optional.empty(), UserNotFoundException.class, "DataSource throws SQLException")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getByUsernameTestcases")
    void testGetByUsername(GetByUsernameTestcase testcase) {
        DataSource dataSource = mock(DataSource.class);
        UserStorageRepository repository = mock(UserStorageRepository.class);
        UserStorageServiceImpl service = new UserStorageServiceImpl(repository, dataSource);
        Connection connection = mock(Connection.class);

        try {
            if (testcase.getConnectionThrows) {
                when(dataSource.getConnection()).thenThrow(new SQLException("DS error"));
            } else {
                when(dataSource.getConnection()).thenReturn(connection);
    
                if (testcase.repoThrowsSQLException) {
                    when(repository.getByUsername(connection, testcase.username)).thenThrow(new SQLException("Repo getByUsername error"));
                } else if (testcase.repoReturnsEmpty) {
                    when(repository.getByUsername(connection, testcase.username)).thenReturn(Optional.empty());
                } else {
                    when(repository.getByUsername(connection, testcase.username)).thenReturn(testcase.repoResult);
                }
            }
    
            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.getByUsername(testcase.username));
            } else {
                User result = assertDoesNotThrow(() -> service.getByUsername(testcase.username));
                assertEquals(testcase.repoResult.get(), result);
                verify(dataSource).getConnection();
                verify(repository).getByUsername(connection, testcase.username);
            } 
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateTestcase(
        User user,
        boolean getConnectionThrows,
        boolean repoThrowsSQLException,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<UpdateTestcase> updateTestcases(){
        return Stream.of(
            new UpdateTestcase(dummyUser, false, false, null, "Valid user - updated successfully"),
            new UpdateTestcase(dummyUser, true, false, UserUpdateException.class, "DataSource throws SQLException"),
            new UpdateTestcase(dummyUser, false, true, UserUpdateException.class, "Repository throws SQLException")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase) {
        DataSource dataSource = mock(DataSource.class);
        UserStorageRepository repository = mock(UserStorageRepository.class);
        UserStorageServiceImpl service = new UserStorageServiceImpl(repository, dataSource);
        Connection connection = mock(Connection.class);

        try {
            if (testcase.getConnectionThrows) {
                when(dataSource.getConnection()).thenThrow(new SQLException("DS error"));
            } else {
                when(dataSource.getConnection()).thenReturn(connection);
                if (testcase.user != null) {
                    if (testcase.repoThrowsSQLException) {
                        doThrow(new SQLException("Repo update error")).when(repository).update(connection, testcase.user);
                    } else {
                        doNothing().when(repository).update(connection, testcase.user);
                    }
                } else {
                    doThrow(new IllegalArgumentException("user must not be null")).when(repository).update(eq(connection), isNull());
                }
            }
    
            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.update(testcase.user));
            } else {
                assertDoesNotThrow(() -> service.update(testcase.user));
                verify(dataSource).getConnection();
                verify(repository).update(connection, testcase.user);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteTestcase(
        String id,
        boolean getConnectionThrows,
        boolean repoThrowsSQLException,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<DeleteTestcase> deleteTestcases(){
        return Stream.of(
            new DeleteTestcase("validId", false, false, null, "Valid id - deleted successfully"),
            new DeleteTestcase("validId", true, false, UserDeleteException.class, "DataSource throws SQLException"),
            new DeleteTestcase("validId", false, true, UserDeleteException.class, "Repository throws SQLException")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testDelete(DeleteTestcase testcase) {
        DataSource dataSource = mock(DataSource.class);
        UserStorageRepository repository = mock(UserStorageRepository.class);
        UserStorageServiceImpl service = new UserStorageServiceImpl(repository, dataSource);
        Connection connection = mock(Connection.class);

        try {
            if (testcase.getConnectionThrows) {
                when(dataSource.getConnection()).thenThrow(new SQLException("DS error"));
            } else {
                when(dataSource.getConnection()).thenReturn(connection);
    
                if (testcase.id == null) {
                    doThrow(new IllegalArgumentException("id must not be null")).when(repository).delete(eq(connection), isNull());
                } else if (testcase.id.isBlank()) {
                    doThrow(new IllegalArgumentException("id must not be blank")).when(repository).delete(connection, testcase.id);
                } else if (testcase.repoThrowsSQLException) {
                    doThrow(new SQLException("Repo delete error")).when(repository).delete(connection, testcase.id);
                } else {
                    doNothing().when(repository).delete(connection, testcase.id);
                }
            }
            
            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.delete(testcase.id));
                verify(dataSource).getConnection();
            } else {
                assertDoesNotThrow(() -> service.delete(testcase.id));
                verify(repository).delete(connection, testcase.id);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }
}
