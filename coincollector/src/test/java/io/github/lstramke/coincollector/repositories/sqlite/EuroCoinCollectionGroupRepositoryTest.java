package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroupFactory;

@ExtendWith(MockitoExtension.class)
public class EuroCoinCollectionGroupRepositoryTest {
    private static final String tableName = "test_groups";
    private static final EuroCoinCollectionGroup dummyCollectionGroup = new EuroCoinCollectionGroup("dummy group", "test_owner");

    record CreateTestcase(
        EuroCoinCollectionGroup group,
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

    static Stream<CreateTestcase> createTestcases(){
        return Stream.of(
            new CreateTestcase(dummyCollectionGroup, mock(Connection.class), false, 1, null, "Valid Group - successful insert"),
            new CreateTestcase(dummyCollectionGroup, mock(Connection.class), false, 0, SQLException.class, "Valid Group - unsuccessful insert"),
            new CreateTestcase(null, mock(Connection.class), false, 1, IllegalArgumentException.class, "Null Group"),
            new CreateTestcase(dummyCollectionGroup, null, false, 1, IllegalArgumentException.class, "Null Connection"),
            new CreateTestcase(dummyCollectionGroup, mock(Connection.class), true, 1, SQLException.class, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(tableName, groupFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.group != null && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.create(testcase.connection, testcase.group),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.create(testcase.connection, testcase.group),
                    "Unexpected exception thrown for: " + testcase.description
                );

                verify(testcase.connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
            
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    record ReadTestcase(
        String id,
        Connection connection,
        boolean shouldThrowSQLException,
        boolean hitInDB,
        boolean factoryThrowsSQLException,
        Optional<EuroCoinCollectionGroup> expectedEuroCoinCollection,
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
            new ReadTestcase("validId", null, false, false, false, Optional.empty(), IllegalArgumentException.class, "Null connection"),
            new ReadTestcase("", mock(Connection.class), false, false, false, Optional.empty(), IllegalArgumentException.class, "Empty id"),
            new ReadTestcase("validId", mock(Connection.class), false, true, false, Optional.of(dummyCollectionGroup), null, "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId", mock(Connection.class), false, true, true, Optional.empty(), null, "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId", mock(Connection.class), true, true, false, Optional.empty(), SQLException.class, "SQLException during read attempt"),
            new ReadTestcase("validId", mock(Connection.class), false, false, false, Optional.empty(), null, "Valid id -  no read hit")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase){
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(tableName, groupFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank() && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if(testcase.shouldThrowSQLException){
                    when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(testcase.hitInDB);

                    if(testcase.hitInDB){
                        if(testcase.factoryThrowsSQLException){
                            when(groupFactory.fromDataBaseEntry(resultSet))
                                .thenThrow(new SQLException("Factory error"));
                        }
                        else if(testcase.expectedEuroCoinCollection.isPresent()){
                            when(groupFactory.fromDataBaseEntry(resultSet))
                                .thenReturn(testcase.expectedEuroCoinCollection.get());
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
                Optional<EuroCoinCollectionGroup> result = repository.read(testcase.connection, testcase.id);
                assertEquals(testcase.expectedEuroCoinCollection, result,
                    "Result value mismatch for: " + testcase.description);

                if (testcase.id != null && !testcase.id.isBlank()) {
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).setString(1, testcase.id);
                    verify(preparedStatement).executeQuery();

                    if (testcase.hitInDB) {
                        verify(resultSet).next();
                        if (!testcase.factoryThrowsSQLException) {
                            verify(groupFactory).fromDataBaseEntry(resultSet);
                        }
                    }
                }
            } 
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }   
    }

    record UpdateTestcase(
        EuroCoinCollectionGroup group,
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
            new UpdateTestcase(dummyCollectionGroup, mock(Connection.class), false, 1, null, "Valid group - successful update"),
            new UpdateTestcase(dummyCollectionGroup, mock(Connection.class), false, 0, SQLException.class, "Valid group  - unsuccessful update"),
            new UpdateTestcase(dummyCollectionGroup, mock(Connection.class), true, 0, SQLException.class, "SQLException during update attempt"),
            new UpdateTestcase(null, mock(Connection.class), false, 0, IllegalArgumentException.class, "Null group"),
            new UpdateTestcase(dummyCollectionGroup, null, false, 0, IllegalArgumentException.class, "Null connection")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(tableName, groupFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.group != null && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.update(testcase.connection, testcase.group),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() -> 
                    repository.update(testcase.connection, testcase.group),
                    "Unexpected exception thrown for: " + testcase.description
                );
                
                verify(testcase.connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }
    
    record DeleteTestcase(
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
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(tableName, groupFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank() && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.delete(testcase.connection, testcase.id),
                "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() -> 
                    repository.delete(testcase.connection, testcase.id),
                    "Unexpected exception thrown for: " + testcase.description
                );
                
                verify(testcase.connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetAllByUserTestcase(
        String userId,
        Connection connection,
        boolean shouldThrowSQLException,
        List<EuroCoinCollectionGroup> collectionsInDB,
        int factoryThrowsOnRow,
        List<EuroCoinCollectionGroup> expectedEuroCoinCollection,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllByUserTestcase> getAllByUserTestcases(){
        EuroCoinCollectionGroup dummyCollectionGroup2 = new EuroCoinCollectionGroup("dummy_group2", "test_owner");
        return Stream.of(
            new GetAllByUserTestcase(null, mock(Connection.class), false, List.of(), -1, List.of(), IllegalArgumentException.class, "Null userId"),
            new GetAllByUserTestcase("validId", null, false, List.of(), -1, List.of(), IllegalArgumentException.class, "Null connection"),
            new GetAllByUserTestcase("", mock(Connection.class), false, List.of(), -1, List.of(), IllegalArgumentException.class, "Blank userId"),
            new GetAllByUserTestcase("test:owner", mock(Connection.class), true, List.of(), -1, List.of(), SQLException.class, "SQLException during select all attempt"),
            new GetAllByUserTestcase("unknown_user", mock(Connection.class), false, List.of(), -1, List.of(), null, "Empty ResultSet"),
            new GetAllByUserTestcase("valid_owner", mock(Connection.class), false, List.of(dummyCollectionGroup), -1, List.of(dummyCollectionGroup), null, "Single Collection"),
            new GetAllByUserTestcase("test_owner", mock(Connection.class), false, List.of(dummyCollectionGroup, dummyCollectionGroup2), -1, List.of(dummyCollectionGroup, dummyCollectionGroup2), 
            null, "Multiple Collection - all valid"),
            new GetAllByUserTestcase("test_owner", mock(Connection.class), false, List.of(dummyCollectionGroup, dummyCollectionGroup2), 1, List.of(dummyCollectionGroup), 
            null, "Multiple Collection - with factory exception")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllByUserTestcases")
    void testGetAllByUser(GetAllByUserTestcase testcase){
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(tableName, groupFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if(testcase.userId != null && !testcase.userId.isBlank() && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if(testcase.shouldThrowSQLException){
                    when(preparedStatement.executeQuery()).thenThrow(new SQLException("Select all failed"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);

                    AtomicInteger row = new AtomicInteger(-1);
                    lenient().when(resultSet.next()).then(hasNext -> row.incrementAndGet() < testcase.collectionsInDB.size());
                    lenient().when(resultSet.getString(eq("collection_id"))).then(collectionId -> testcase.collectionsInDB.get(row.get()).getId());
                    lenient().when(groupFactory.fromDataBaseEntry(resultSet)).then(group -> {
                        int i = row.get();
                        if(testcase.factoryThrowsOnRow == i){
                            throw new SQLException("factory exception");
                        } else {
                            return testcase.collectionsInDB.get(i);
                        }
                    });
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.getAllByUser(testcase.connection, testcase.userId),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                List<EuroCoinCollectionGroup> result = repository.getAllByUser(testcase.connection, testcase.userId);
                assertEquals(testcase.expectedEuroCoinCollection, result,
                    "Result value mismatch for: " + testcase.description
                );

                verify(testcase.connection).prepareStatement(anyString());
                verify(preparedStatement).executeQuery();

                if (!testcase.collectionsInDB.isEmpty()) {
                    verify(resultSet, atLeastOnce()).next();
                }
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());        
        }
    }

    private record ExistsTestcase(
        String coinId,
        Connection connection,
        boolean resultSetHasNext, 
        boolean expectedResult,
        Class<? extends Exception> expectedException,
        boolean shouldThrowSQLException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<ExistsTestcase> existsTestcases(){
        return Stream.of(
            new ExistsTestcase("validId", mock(Connection.class), true, true, null, false, "Collection exists"),
            new ExistsTestcase("non-existing-id", mock(Connection.class), false, false, null, false, "Collection does not exist"),
            new ExistsTestcase(null, mock(Connection.class), false, false, IllegalArgumentException.class, false, "Null ID"),
            new ExistsTestcase("validId", null, false, false, IllegalArgumentException.class, false, "Null Connection"),
            new ExistsTestcase("", mock(Connection.class), false, false, IllegalArgumentException.class, false, "Empty ID"),
            new ExistsTestcase("  ", mock(Connection.class), false, false, IllegalArgumentException.class, false, "Whitespace-only ID"),
            new ExistsTestcase("db-error-id", mock(Connection.class), false, false, SQLException.class, true, "Database error")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestcases")
    void testExists(ExistsTestcase testcase){
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(tableName, groupFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if (testcase.coinId() != null && !testcase.coinId().isBlank() && testcase.connection != null) {
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
                    repository.exists(testcase.connection, testcase.coinId),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                boolean result = repository.exists(testcase.connection, testcase.coinId);

                assertEquals(testcase.expectedResult, result, 
                    "Result value mismatch for: " + testcase.description
                );

                if (testcase.coinId!= null && !testcase.coinId.isBlank()) {
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).setString(1, testcase.coinId);
                    verify(preparedStatement).executeQuery();
                    verify(resultSet).next();
                }
            } 
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record ValidationTestcase(
        String id,
        String ownerId,
        List<EuroCoinCollection> collections,
        boolean expectedResult,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<ValidationTestcase> validationTestcases(){
        return Stream.of(
            new ValidationTestcase(null, null, null, false, "null group"),
            new ValidationTestcase(null, "test_owner", List.of(), false, "id null"),
            new ValidationTestcase("", "test_owner", List.of(), false, "id empty"),
            new ValidationTestcase("   ", "test_owner", List.of(), false, "id blank"),
            new ValidationTestcase("valid-id", null, List.of(), false, "null ownerId"),
            new ValidationTestcase("valid-id", "", List.of(), false, "empty ownerId"),
            new ValidationTestcase("valid-id", "    ", List.of(), false, "blank ownerId"),
            new ValidationTestcase("valid-id", "test_owner", null, false, "coins list null"),
            new ValidationTestcase("valid-id-empty-coins", "test_owner", List.of(), true, "valid group - empty coins list"),
            new ValidationTestcase("valid-id-with-coin", "test_owner", List.of(mock(EuroCoinCollection.class)), true, "valid group - non empty coins list")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validationTestcases")
    void testValidateEuroCoinCollectionGroup(ValidationTestcase testcase){
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(tableName, groupFactory);

        EuroCoinCollectionGroup group = null;
        if(!testcase.description.contains("null group")){
            group = mock(EuroCoinCollectionGroup.class);
            lenient().when(group.getId()).thenReturn(testcase.id);
            lenient().when(group.getOwnerId()).thenReturn(testcase.ownerId);
            lenient().when(group.getCollections()).thenReturn(testcase.collections);
        }

        Boolean result = repository.validateEuroCoinCollectionGroup(group);

        assertEquals(testcase.expectedResult(), result,
                "Result value mismatch for: " + testcase.description);
    }
}
