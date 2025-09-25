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

import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionFactory;

@ExtendWith(MockitoExtension.class)
public class EuroCoinCollectionSqliteRepositoryTest {
    

    private static final String tableName = "test_collections";
    private static final EuroCoinCollection dummyCollection = new EuroCoinCollection("dummy collection", "test_group");

    record CreateTestcase(
        EuroCoinCollection collection,
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
            new CreateTestcase(dummyCollection, mock(Connection.class), false, 1, null, "Valid Collection - successful insert"),
            new CreateTestcase(dummyCollection, mock(Connection.class), false, 0, SQLException.class, "Valid Collection - unsuccessful insert"),
            new CreateTestcase(null, mock(Connection.class), false, 1, IllegalArgumentException.class, "Null Collection"),
            new CreateTestcase(dummyCollection, null, false, 1, IllegalArgumentException.class, "Null Connection"),
            new CreateTestcase(dummyCollection, mock(Connection.class), true, 1, SQLException.class, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.collection != null && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.create(testcase.connection, testcase.collection),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.create(testcase.connection, testcase.collection),
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
        Optional<EuroCoinCollection> expectedEuroCoinCollection,
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
            new ReadTestcase("validId", mock(Connection.class), false, true, false, Optional.of(dummyCollection), null, "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId", mock(Connection.class), false, true, true, Optional.empty(), null, "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId", mock(Connection.class), true, true, false, Optional.empty(), SQLException.class, "SQLException during read attempt"),
            new ReadTestcase("validId", mock(Connection.class), false, false, false, Optional.empty(), null, "Valid id -  no read hit")
        );
    }
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

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
                            when(collectionFactory.fromDataBaseEntry(resultSet))
                                .thenThrow(new SQLException("Factory error"));
                        }
                        else if(testcase.expectedEuroCoinCollection.isPresent()){
                            when(collectionFactory.fromDataBaseEntry(resultSet))
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
                Optional<EuroCoinCollection> result = repository.read(testcase.connection, testcase.id);
                assertEquals(testcase.expectedEuroCoinCollection, result,
                    "Result value mismatch for: " + testcase.description);

                if (testcase.id != null && !testcase.id.isBlank()) {
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).setString(1, testcase.id);
                    verify(preparedStatement).executeQuery();

                    if (testcase.hitInDB) {
                        verify(resultSet).next();
                        if (!testcase.factoryThrowsSQLException) {
                            verify(collectionFactory).fromDataBaseEntry(resultSet);
                        }
                    }
                }
            }         
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }       
    }

    record UpdateTestcase(
        EuroCoinCollection collection,
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
            new UpdateTestcase(dummyCollection, mock(Connection.class), false, 1, null, "Valid collection - successful update"),
            new UpdateTestcase(dummyCollection, mock(Connection.class), false, 0, SQLException.class, "Valid collection  - unsuccessful update"),
            new UpdateTestcase(dummyCollection, mock(Connection.class), true, 0, SQLException.class, "SQLException during update attempt"),
            new UpdateTestcase(null, mock(Connection.class), false, 0, IllegalArgumentException.class, "Null collection "),
            new UpdateTestcase(dummyCollection, null, false, 0, IllegalArgumentException.class, "Null connection ")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.collection != null && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () ->
                    repository.update(testcase.connection, testcase.collection),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() -> 
                    repository.update(testcase.connection, testcase.collection),
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
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

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

    private record GetAllTestcase(
        Connection connection,
        boolean shouldThrowSQLException,
        List<EuroCoinCollection> collectionsInDB,
        int factoryThrowsOnRow,
        List<EuroCoinCollection> expectedEuroCoinCollection,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllTestcase> getAllTestcases(){
        EuroCoinCollection dummyCoinCollection2 = new EuroCoinCollection("dummy_collection2", "amazing_group");
        return Stream.of(
            new GetAllTestcase(null, false, List.of(), -1, List.of(), IllegalArgumentException.class, "Null connection"),
            new GetAllTestcase(mock(Connection.class), true, List.of(), -1, List.of(), SQLException.class, "SQLException during select all attempt"),
            new GetAllTestcase(mock(Connection.class), false, List.of(), -1, List.of(), null, "Empty ResultSet"),
            new GetAllTestcase(mock(Connection.class), false, List.of(dummyCollection), -1, List.of(dummyCollection), null, "Single Collection"),
            new GetAllTestcase(mock(Connection.class), false, List.of(dummyCollection, dummyCoinCollection2), -1, List.of(dummyCollection, dummyCoinCollection2), null, 
            "Multiple Collection - all valid"),
            new GetAllTestcase(mock(Connection.class), false, List.of(dummyCollection, dummyCoinCollection2), 1, List.of(dummyCollection), null, 
            "Multiple Collection - with factory exception")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllTestcases")
    void testGetAll(GetAllTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if(testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if(testcase.shouldThrowSQLException){
                    when(preparedStatement.executeQuery()).thenThrow(new SQLException("Select all failed"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);

                    AtomicInteger row = new AtomicInteger(-1);
                    lenient().when(resultSet.next()).then(hasNext -> row.incrementAndGet() < testcase.collectionsInDB.size());
                    lenient().when(resultSet.getString(eq("collection_id"))).then(collectionId -> testcase.collectionsInDB.get(row.get()).getId());
                    lenient().when(collectionFactory.fromDataBaseEntry(resultSet)).then(collection -> {
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
                    repository.getAll(testcase.connection),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                List<EuroCoinCollection> result = repository.getAll(testcase.connection);
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
            new ExistsTestcase("valid-id", mock(Connection.class), true, true, null, false, "Collection exists"),
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
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

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
        List<EuroCoin> coins,
        String groupId,
        boolean expectedResult,
        boolean isNullCollection,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<ValidationTestcase> validationTestcases(){
        return Stream.of(
            new ValidationTestcase(null, null, null, false, true, "null collection"),
            new ValidationTestcase(null, List.of(),"valid_owner_id", false, false, "id null"),
            new ValidationTestcase("", List.of(),"valid_owner_id", false, false, "id empty"),
            new ValidationTestcase("   ", List.of(), "valid_owner_id", false, false, "id blank"),
            new ValidationTestcase("valid-id", null, "valid_owner_id", false, false, "coins list null"),
            new ValidationTestcase("valid-id", List.of(), null, false, false, "ownerId null"),
            new ValidationTestcase("valid-id", List.of(), "", false, false, "ownerId blank"),
            new ValidationTestcase("valid-id", List.of(), "valid_owner_id", true, false, "valid collection - empty coins list"),
            new ValidationTestcase("valid-id", List.of(mock(EuroCoin.class)), "valid_owner_id", true, false, "valid collection - non empty coins list")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validationTestcases")
    void testValidateEuroCoinCollection(ValidationTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        EuroCoinCollection collection = null;
        if(!testcase.isNullCollection){
            collection = mock(EuroCoinCollection.class);
            lenient().when(collection.getId()).thenReturn(testcase.id);
            lenient().when(collection.getCoins()).thenReturn(testcase.coins);
            lenient().when(collection.getGroupId()).thenReturn(testcase.groupId);
        }

        Boolean result = repository.validateEuroCoinCollection(collection);

        assertEquals(testcase.expectedResult(), result,
                "Result value mismatch for: " + testcase.description);
    }
}
