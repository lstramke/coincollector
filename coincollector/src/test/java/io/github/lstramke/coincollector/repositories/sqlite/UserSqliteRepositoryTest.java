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

import javax.sql.DataSource;

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

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(dataSource, tableName, userFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try{
            if(testcase.user != null){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            boolean result = repository.create(testcase.user());

            assertEquals(testcase.expectedResult, result,
                        "Result value mismatch for: " + testcase.description);

            if (testcase.user != null && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }

        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record CreateTestcase(
        User user,
        boolean shouldThrowSQLException,
        int rowsAffected,
        boolean expectedResult,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<CreateTestcase> createTestcases(){
        return Stream.of(
            new CreateTestcase(dummyUser, false, 1, true, "Valid User - successful insert"),
            new CreateTestcase(dummyUser, false, 0, false, "Valid User - unsuccessful insert"),
            new CreateTestcase(null, false, 1, false, "Null User"),
            new CreateTestcase(dummyUser, true, 1, false, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(dataSource, tableName, userFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank()){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if(testcase.shouldThrowSQLException){
                    when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(testcase.hitInDB);

                    if(testcase.hitInDB){
                        if(testcase.factoryThrowsSQLException){
                            when(userFactory.fromDataBaseEntry(resultSet))
                                .thenThrow(new SQLException("Factory error"));
                        }
                        else if(testcase.expectedUser.isPresent()){
                            when(userFactory.fromDataBaseEntry(resultSet))
                                .thenReturn(testcase.expectedUser.get());
                        }
                    }
                }
            }

            Optional<User> result = repository.read(testcase.id);

            assertEquals(testcase.expectedUser, result,                         
                        "Result value mismatch for: " + testcase.description);
            if (testcase.id != null && !testcase.id.isBlank() && !testcase.shouldThrowSQLException && testcase.hitInDB) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).setString(1, testcase.id);
                verify(preparedStatement).executeQuery();
                verify(resultSet).next();
                if (!testcase.factoryThrowsSQLException) {
                    verify(userFactory).fromDataBaseEntry(resultSet);
                }
            }
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record ReadTestcase(
        String id,
        boolean shouldThrowSQLException,
        boolean hitInDB,
        boolean factoryThrowsSQLException,
        Optional<User> expectedUser,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<ReadTestcase> readTestcases(){
        return Stream.of(
            new ReadTestcase(null, false, false, false, Optional.empty(), "Null id"),
            new ReadTestcase("", false, false, false, Optional.empty(), "Empty id"),
            new ReadTestcase("validId", false, true, false, Optional.of(dummyUser), "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId", false, true, true, Optional.empty(), "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId", true, true, false, Optional.empty(), "SQLException during read attempt"),
            new ReadTestcase("validId", false, false, false, Optional.empty(), "Valid id - no read hit")
        ); 
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(dataSource, tableName, userFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.user != null){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            boolean result = repository.update(testcase.user);

            assertEquals(testcase.expectedResult, result,
                        "Result value mismatch for: " + testcase.description);

            if (testcase.user != null && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateTestcase(
        User user,
        boolean shouldThrowSQLException,
        int rowsAffected,
        boolean expectedResult,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<UpdateTestcase> updateTestcases(){
        return Stream.of(
            new UpdateTestcase(dummyUser, false, 1, true, "Valid user - successful update"),
            new UpdateTestcase(dummyUser, false, 0, false, "Valid user - unsuccessful update"),
            new UpdateTestcase(dummyUser, true, 0, false, "SQLException during update attempt"),
            new UpdateTestcase(null, false, 0, false, "Null user")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testDelete(DeleteTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(dataSource, tableName, userFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank()){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Delete failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            boolean result = repository.delete(testcase.id);

            assertEquals(testcase.expectedResult, result,
                        "Result value mismatch for: " + testcase.description);

            if (testcase.id != null && !testcase.id.isBlank() && !testcase.shouldThrowSQLException) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
            
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteTestcase(
        String id,
        boolean shouldThrowSQLException,
        int rowsAffected,
        boolean expectedResult,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<DeleteTestcase> deleteTestcases(){
        return Stream.of(
            new DeleteTestcase(null, false, 0, false, "Null Id"),
            new DeleteTestcase("", false, 0, false, "Empty Id"),
            new DeleteTestcase("validId", false, 1, true, "Valid Id - successful delete"),
            new DeleteTestcase("validId", false, 0, false, "Valid Id - unsuccessful delete"),
            new DeleteTestcase("validId", true, 0, false, "SQLException during delete attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestcases")
    void testExists(ExistsTestcase testcase) {
        DataSource dataSource = mock(DataSource.class);
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(dataSource, tableName, userFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if (testcase.userId() != null && !testcase.userId().trim().isEmpty()) {
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeQuery())
                            .thenThrow(new SQLException("Database connection failed"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(testcase.resultSetHasNext());
                }
            }

            Optional<Boolean> result = repository.exists(testcase.userId());

            assertEquals(testcase.expectedResult(), result,
                    "Optional / Wert mismatch für: " + testcase.description);

            if (testcase.userId() != null && !testcase.userId().trim().isEmpty()
                    && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).setString(1, testcase.userId());
                verify(preparedStatement).executeQuery();
                verify(resultSet).next();
            }

        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record ExistsTestcase(
        String userId, 
        boolean resultSetHasNext, 
        Optional<Boolean> expectedResult,
        boolean shouldThrowSQLException,
        String description) {

        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ExistsTestcase> existsTestcases() {
        return Stream.of(
            new ExistsTestcase("valid-id", true, Optional.of(true), false, "User exists"),
            new ExistsTestcase("non-existing-id", false, Optional.of(false), false, "User does not exist"),
            new ExistsTestcase(null, false, Optional.of(false), false, "Null ID"),
            new ExistsTestcase("", false, Optional.of(false), false, "Empty ID"),
            new ExistsTestcase("  ", false, Optional.of(false), false, "Whitespace-only ID"),
            new ExistsTestcase("db-error-id", false, Optional.empty(), true, "Database error")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validateUserTestcases")
    void testValidateUser(ValidationTestcase testcase) {
        DataSource dataSource = mock(DataSource.class);
        UserFactory userFactory = mock(UserFactory.class);
        UserSqliteRepository repository = new UserSqliteRepository(dataSource, tableName, userFactory);

        User user = null;
        if (!testcase.isNullUser()) {
            user = mock(User.class);
            lenient().when(user.getId()).thenReturn(testcase.userId());
        }

        boolean result = repository.validateUser(user);

        assertEquals(testcase.expectedResult(), result,
                "Validation result mismatch for: " + testcase.description);
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
}
