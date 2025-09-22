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
            new CreateTestcase(dummyCollection, false, 1, null, "Valid Collection - successful insert"),
            new CreateTestcase(dummyCollection, false, 0, SQLException.class, "Valid Collection - unsuccessful insert"),
            new CreateTestcase(null, false, 1, IllegalArgumentException.class, "Null Collection"),
            new CreateTestcase(dummyCollection, true, 1, SQLException.class, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.collection != null){
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.create(connection, testcase.collection),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.create(connection, testcase.collection),
                    "Unexpected exception thrown for: " + testcase.description
                );

                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }          
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }


    record ReadTestcase(
        String id,
        boolean shouldThrowSQLException,
        boolean hitInDB,
        boolean factoryThrowsSQLException,
        Optional<EuroCoinCollection> expectedEuroCoinCollection,
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
            new ReadTestcase("validId", false, true, false, Optional.of(dummyCollection), "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId", false, true, true, Optional.empty(), "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId", true, true, false, Optional.empty(), "SQLException during read attempt"),
            new ReadTestcase("validId", false, false, false, Optional.empty(), "Valid id -  no read hit")
        );
    }
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank()){
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

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

            if (testcase.shouldThrowSQLException) {
                assertThrows(SQLException.class,
                    () -> repository.read(connection, testcase.id),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                Optional<EuroCoinCollection> result = repository.read(connection, testcase.id);
                assertEquals(testcase.expectedEuroCoinCollection, result,
                    "Result value mismatch for: " + testcase.description);

                if (testcase.id != null && !testcase.id.isBlank()) {
                    verify(connection).prepareStatement(anyString());
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
            new UpdateTestcase(dummyCollection, false, 1, null, "Valid collection - successful update"),
            new UpdateTestcase(dummyCollection, false, 0, SQLException.class, "Valid collection  - unsuccessful update"),
            new UpdateTestcase(dummyCollection, true, 0, SQLException.class, "SQLException during update attempt"),
            new UpdateTestcase(null, false, 0, IllegalArgumentException.class, "Null collection ")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.collection != null){
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () ->
                    repository.update(connection, testcase.collection),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() -> 
                    repository.update(connection, testcase.collection),
                     "Unexpected exception thrown for: " + testcase.description
                );
                
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    record DeleteTestcase(
        String id,
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
            new DeleteTestcase(null, false, 0, IllegalArgumentException.class, "Null Id"),
            new DeleteTestcase("", false, 0, IllegalArgumentException.class, "Empty Id"),
            new DeleteTestcase("validId", false, 1, null, "Valid Id - successful delete"),
            new DeleteTestcase("validId", false, 0, SQLException.class, "Valid Id - unsuccessful delete"),
            new DeleteTestcase("validId", true, 0, SQLException.class, "SQLException during delete attempt")
        );
    }
    
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testDelete(DeleteTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank()){
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> 
                    repository.delete(connection, testcase.id),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.delete(connection, testcase.id),
                     "Unexpected exception thrown for: " + testcase.description
                );

                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetAllTestcase(
        boolean shouldThrowSQLException,
        List<EuroCoinCollection> collectionsInDB,
        int factoryThrowsOnRow,
        List<EuroCoinCollection> expectedEuroCoinCollection,
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
            new GetAllTestcase(true, List.of(), -1, List.of(), "SQLException during select all attempt"),
            new GetAllTestcase(false, List.of(), -1, List.of(), "Empty ResultSet"),
            new GetAllTestcase(false, List.of(dummyCollection), -1, List.of(dummyCollection), "Single Collection"),
            new GetAllTestcase(false, List.of(dummyCollection, dummyCoinCollection2), -1, List.of(dummyCollection, dummyCoinCollection2), 
            "Multiple Collection - all valid"),
            new GetAllTestcase(false, List.of(dummyCollection, dummyCoinCollection2), 1, List.of(dummyCollection), 
            "Multiple Collection - with factory exception")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllTestcases")
    void testGetAll(GetAllTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

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

           if(testcase.shouldThrowSQLException){
                assertThrows(SQLException.class, () ->
                    repository.getAll(connection),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                List<EuroCoinCollection> result = repository.getAll(connection);
                assertEquals(testcase.expectedEuroCoinCollection, result,
                    "Result value mismatch for: " + testcase.description
                );

                verify(connection).prepareStatement(anyString());
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
            new ExistsTestcase("valid-id", true, true, null, false, "Collection exists"),
            new ExistsTestcase("non-existing-id", false, false, null, false, "Collection does not exist"),
            new ExistsTestcase(null, false, false, IllegalArgumentException.class, false, "Null ID"),
            new ExistsTestcase("", false, false, IllegalArgumentException.class, false, "Empty ID"),
            new ExistsTestcase("  ", false, false, IllegalArgumentException.class, false, "Whitespace-only ID"),
            new ExistsTestcase("db-error-id", false, false, SQLException.class, true, "Database error")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestcases")
    void testExists(ExistsTestcase testcase){
        EuroCoinCollectionFactory collectionFactory = mock(EuroCoinCollectionFactory.class);
        EuroCoinCollectionSqliteRepository repository = new EuroCoinCollectionSqliteRepository(tableName, collectionFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if (testcase.coinId() != null && !testcase.coinId().trim().isEmpty()) {
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
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
                    repository.exists(connection, testcase.coinId),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                boolean result = repository.exists(connection, testcase.coinId);

                assertEquals(testcase.expectedResult, result, 
                    "Result value mismatch for: " + testcase.description
                );

                if (testcase.coinId!= null && !testcase.coinId.isBlank()) {
                    verify(connection).prepareStatement(anyString());
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
