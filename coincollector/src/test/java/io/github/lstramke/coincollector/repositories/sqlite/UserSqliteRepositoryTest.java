package io.github.lstramke.coincollector.repositories.sqlite;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.UserFactory;

@ExtendWith(MockitoExtension.class)
public class UserSqliteRepositoryTest {
    private static final String tableName = "test_users";
    private static final User dummyUser = new User("Bob");


    private record CreateTestcase(
        User user,
        Connection connection,
        boolean shouldThrowSQLException,
        int rowsAffected,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<CreateTestcase> createTestcases(){
        return Stream.of(
            new CreateTestcase(dummyUser, mock(Connection.class), false, 1, null, "Valid User - successful insert"),
            new CreateTestcase(dummyUser, mock(Connection.class), false, 0, SQLException.class, "Valid User - unsuccessful insert"),
            new CreateTestcase(null, mock(Connection.class), false, 1, IllegalArgumentException.class, "Null User"),
            new CreateTestcase(dummyUser, null, false, 1, IllegalArgumentException.class, "Null Connection"),
            new CreateTestcase(dummyUser, mock(Connection.class), true, 1, SQLException.class, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(tableName, userFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        try{
            if (testcase.user != null && testcase.connection != null) {
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () ->
                    repository.create(testcase.connection, testcase.user),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.create(testcase.connection, testcase.user),
                    "Unexpected exception thrown for: " + testcase.description
                );
            
                verify(testcase.connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record ReadTestcase(
        String id,
        Connection connection,
        boolean shouldThrowSQLException,
        boolean hitInDB,
        boolean factoryThrowsSQLException,
        Optional<User> expectedUser,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<ReadTestcase> readTestcases(){
        return Stream.of(
            new ReadTestcase(null, mock(Connection.class), false, false, false, Optional.empty(), IllegalArgumentException.class, "Null id"),
            new ReadTestcase("validId", null, false, false, false, Optional.empty(), IllegalArgumentException.class, "Null Connection"),
            new ReadTestcase("", mock(Connection.class), false, false, false, Optional.empty(), IllegalArgumentException.class, "Empty id"),
            new ReadTestcase("validId", mock(Connection.class), false, true, false, Optional.of(dummyUser), null, "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId", mock(Connection.class), false, true, true, Optional.empty(), SQLException.class, "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId", mock(Connection.class), true, true, false, Optional.empty(), SQLException.class, "SQLException during read attempt"),
            new ReadTestcase("validId", mock(Connection.class), false, false, false, Optional.empty(), null, "Valid id - no read hit")
        ); 
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase) throws SQLException {
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(tableName, userFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        try{
            if (testcase.id != null && !testcase.id.isBlank() && testcase.connection != null) {
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException) {
                    when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(testcase.hitInDB);

                    if (testcase.hitInDB) {
                        if (testcase.factoryThrowsSQLException) {
                            when(userFactory.fromDataBaseEntry(resultSet))
                                .thenThrow(new SQLException("Factory error"));
                        } else if (testcase.expectedUser.isPresent()) {
                            when(userFactory.fromDataBaseEntry(resultSet))
                                .thenReturn(testcase.expectedUser.get());
                        }
                    }
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException,
                    () -> repository.read(testcase.connection, testcase.id),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                Optional<User> result = repository.read(testcase.connection, testcase.id);
                assertEquals(testcase.expectedUser, result,
                    "Result value mismatch for: " + testcase.description);

                if (testcase.id != null && !testcase.id.isBlank() && testcase.connection != null) {
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).setString(1, testcase.id);
                    verify(preparedStatement).executeQuery();

                    if (testcase.hitInDB) {
                        verify(resultSet).next();
                        if (!testcase.factoryThrowsSQLException) {
                            verify(userFactory).fromDataBaseEntry(resultSet);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateTestcase(
        User user,
        Connection connection,
        boolean shouldThrowSQLException,
        int rowsAffected,
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
            new UpdateTestcase(dummyUser, mock(Connection.class), false, 1, null, "Valid user - successful update"),
            new UpdateTestcase(dummyUser, mock(Connection.class), false, 0, SQLException.class, "Valid user - unsuccessful update"),
            new UpdateTestcase(dummyUser, mock(Connection.class), true, 0, SQLException.class, "SQLException during update attempt"),
            new UpdateTestcase(null, mock(Connection.class), false, 0, IllegalArgumentException.class, "Null user"),
            new UpdateTestcase(dummyUser, null, false, 0, IllegalArgumentException.class, "Null Connection")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(tableName, userFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.user != null && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () ->
                    repository.update(testcase.connection, testcase.user),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.update(testcase.connection, testcase.user),
                    "Unexpected exception thrown for: " + testcase.description
                );
            
                if(testcase.connection != null){
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).executeUpdate();
                }
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteTestcase(
        String id,
        Connection connection,
        boolean shouldThrowSQLException,
        int rowsAffected,
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
            new DeleteTestcase(null, mock(Connection.class), false, 0, IllegalArgumentException.class, "Null Id"),
            new DeleteTestcase("validId", null, false, 0, IllegalArgumentException.class, "Null Connection"),
            new DeleteTestcase("", mock(Connection.class), false, 0, IllegalArgumentException.class, "Empty Id"),
            new DeleteTestcase("validId", mock(Connection.class), false, 1, null, "Valid Id - successful delete"),
            new DeleteTestcase("validId", mock(Connection.class), false, 0, SQLException.class, "Valid Id - unsuccessful delete"),
            new DeleteTestcase("validId", mock(Connection.class), true, 0, SQLException.class, "SQLException during delete attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testDelete(DeleteTestcase testcase){
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(tableName, userFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank() && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Delete failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.delete(testcase.connection, testcase.id),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.delete(testcase.connection, testcase.id),
                    "Unexpected exception thrown for: " + testcase.description
                );

                if(testcase.connection != null){
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).executeUpdate();
                }
            }

            
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record ExistsTestcase(
        String userId,
        Connection connection,
        boolean resultSetHasNext, 
        boolean expectedResult,
        Class<? extends Exception> expectedException,
        boolean shouldThrowSQLException,
        String description) {

        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ExistsTestcase> existsTestcases() {
        return Stream.of(
            new ExistsTestcase("valid-id", mock(Connection.class), true, true, null, false, "User exists"),
            new ExistsTestcase("non-existing-id", mock(Connection.class), false, false, null, false, "User does not exist"),
            new ExistsTestcase(null, mock(Connection.class), false, false, IllegalArgumentException.class, false, "Null ID"),
            new ExistsTestcase("valid-id", null, false, false, IllegalArgumentException.class, false, "Null Connection"),
            new ExistsTestcase("", mock(Connection.class), false, false, IllegalArgumentException.class, false, "Empty ID"),
            new ExistsTestcase("  ", mock(Connection.class), false, false, IllegalArgumentException.class, false, "Whitespace-only ID"),
            new ExistsTestcase("db-error-id", mock(Connection.class), false, false, SQLException.class, true, "Database error")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestcases")
    void testExists(ExistsTestcase testcase) {
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(tableName, userFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if (testcase.userId() != null && !testcase.userId().isBlank() && testcase.connection != null) {
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);
                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeQuery())
                            .thenThrow(new SQLException("Database connection failed"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(testcase.resultSetHasNext());
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.exists(testcase.connection, testcase.userId),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                boolean result = repository.exists(testcase.connection, testcase.userId);

                assertEquals(testcase.expectedResult, result, 
                    "Result value mismatch for: " + testcase.description
                );

                if (testcase.userId != null && !testcase.userId.isBlank() && testcase.connection != null) {
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).setString(1, testcase.userId());
                    verify(preparedStatement).executeQuery();
                    verify(resultSet).next();
                }
            }   
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record ValidationTestcase(
        boolean isNullUser,
        String userId,
        boolean expectedResult,
        String description) {

        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ValidationTestcase> validateUserTestcases() {
        return Stream.of(
            new ValidationTestcase(false, "valid-id", true, "Valid user"),
            new ValidationTestcase(true, null, false, "Null user"),
            new ValidationTestcase(false, null, false, "User with null ID"),
            new ValidationTestcase(false, "", false, "User with empty ID"),
            new ValidationTestcase(false, "   ", false, "User with whitespace-only ID")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validateUserTestcases")
    void testValidateUser(ValidationTestcase testcase) {
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(tableName, userFactory);

        User user = null;
        if (!testcase.isNullUser) {
            user = mock(User.class);
            lenient().when(user.getId()).thenReturn(testcase.userId());
        }

        boolean result = repository.validateUser(user);

        assertEquals(testcase.expectedResult(), result,
                "Validation result mismatch for: " + testcase.description);
    }
}
