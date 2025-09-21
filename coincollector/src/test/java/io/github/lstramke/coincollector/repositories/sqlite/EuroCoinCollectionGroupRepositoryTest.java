package io.github.lstramke.coincollector.repositories.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(dataSource, tableName, groupFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.group != null){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            boolean result = repository.create(testcase.group);

            assertEquals(testcase.expectedResult, result,
                        "Result value mismatch for: " + testcase.description);

            if (testcase.group != null && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
            
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    record CreateTestcase(
        EuroCoinCollectionGroup group,
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

    static Stream<CreateTestcase> createTestcases(){
        return Stream.of(
            new CreateTestcase(dummyCollectionGroup, false, 1, true, "Valid Group - successful insert"),
            new CreateTestcase(dummyCollectionGroup, false, 0, false, "Valid Group - unsuccessful insert"),
            new CreateTestcase(null, false, 1, false, "Null Group"),
            new CreateTestcase(dummyCollectionGroup, true, 1, false, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(dataSource, tableName, groupFactory);

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

            Optional<EuroCoinCollectionGroup> result = repository.read(testcase.id);

            assertEquals(testcase.expectedEuroCoinCollection, result,
                        "Result value mismatch for: " + testcase.description);
            
            if (testcase.id != null && !testcase.id.isBlank() && !testcase.shouldThrowSQLException && testcase.hitInDB) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).setString(1, testcase.id);
                verify(preparedStatement).executeQuery();
                verify(resultSet).next();
                if (!testcase.factoryThrowsSQLException) {
                    verify(groupFactory).fromDataBaseEntry(resultSet);
                    }
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
        Optional<EuroCoinCollectionGroup> expectedEuroCoinCollection,
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
            new ReadTestcase("validId", false, true, false, Optional.of(dummyCollectionGroup), "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId", false, true, true, Optional.empty(), "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId", true, true, false, Optional.empty(), "SQLException during read attempt"),
            new ReadTestcase("validId", false, false, false, Optional.empty(), "Valid id -  no read hit")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(dataSource, tableName, groupFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.group != null){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            boolean result = repository.update(testcase.group);

            assertEquals(testcase.expectedResult, result,                         
                        "Result value mismatch for: " + testcase.description);

            if (testcase.group != null && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
            
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    record UpdateTestcase(
        EuroCoinCollectionGroup group,
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
            new UpdateTestcase(dummyCollectionGroup, false, 1, true, "Valid group - successful update"),
            new UpdateTestcase(dummyCollectionGroup, false, 0, false, "Valid group  - unsuccessful update"),
            new UpdateTestcase(dummyCollectionGroup, true, 0, false, "SQLException during update attempt"),
            new UpdateTestcase(null, false, 0, false, "Null group ")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testDelete(DeleteTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(dataSource, tableName, groupFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.id != null && !testcase.id.isBlank()){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
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

    record DeleteTestcase(
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
    @MethodSource("getAllByUserTestcases")
    void testGetAllByUser(GetAllByUserTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(dataSource, tableName, groupFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if(testcase.userId != null && !testcase.userId.isBlank()){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

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

            List<EuroCoinCollectionGroup> result = repository.getAllByUser(testcase.userId);

            assertEquals(testcase.expectedEuroCoinCollection, result, 
                        "Result value mismatch for: " + testcase.description);
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());        
        }

    }

    private record GetAllByUserTestcase(
        String userId,
        boolean shouldThrowSQLException,
        List<EuroCoinCollectionGroup> collectionsInDB,
        int factoryThrowsOnRow,
        List<EuroCoinCollectionGroup> expectedEuroCoinCollection,
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
            new GetAllByUserTestcase(null, false, List.of(), -1, List.of(), "Null userId"),
            new GetAllByUserTestcase("", false, List.of(), -1, List.of(), "Blank userId"),
            new GetAllByUserTestcase("test:owner", true, List.of(), -1, List.of(), "SQLException during select all attempt"),
            new GetAllByUserTestcase("unknown_user", false, List.of(), -1, List.of(), "Empty ResultSet"),
            new GetAllByUserTestcase("valid_owner", false, List.of(dummyCollectionGroup), -1, List.of(dummyCollectionGroup), "Single Collection"),
            new GetAllByUserTestcase("test_owner", false, List.of(dummyCollectionGroup, dummyCollectionGroup2), -1, List.of(dummyCollectionGroup, dummyCollectionGroup2), 
            "Multiple Collection - all valid"),
            new GetAllByUserTestcase("test_owner", false, List.of(dummyCollectionGroup, dummyCollectionGroup2), 1, List.of(dummyCollectionGroup), 
            "Multiple Collection - with factory exception")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestcases")
    void testExists(ExistsTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(dataSource, tableName, groupFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            if (testcase.coinId() != null && !testcase.coinId().trim().isEmpty()) {
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

            Optional<Boolean> result = repository.exists(testcase.coinId());

            assertEquals(testcase.expectedResult(), result,
                    "Result value mismatch for: " + testcase.description);

            if (testcase.coinId() != null && !testcase.coinId().trim().isEmpty()
                    && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).setString(1, testcase.coinId());
                verify(preparedStatement).executeQuery();
                verify(resultSet).next();
            }

        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record ExistsTestcase(
        String coinId, 
        boolean resultSetHasNext, 
        Optional<Boolean> expectedResult,
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
            new ExistsTestcase("valid-id", true, Optional.of(true), false, "Collection exists"),
            new ExistsTestcase("non-existing-id", false, Optional.of(false), false, "Collection does not exist"),
            new ExistsTestcase(null, false, Optional.of(false), false, "Null ID"),
            new ExistsTestcase("", false, Optional.of(false), false, "Empty ID"),
            new ExistsTestcase("  ", false, Optional.of(false), false, "Whitespace-only ID"),
            new ExistsTestcase("db-error-id", false, Optional.empty(), true, "Database error")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validationTestcases")
    void testValidateEuroCoinCollectionGroup(ValidationTestcase testcase){
        DataSource dataSource = mock(DataSource.class);
        EuroCoinCollectionGroupFactory groupFactory = mock(EuroCoinCollectionGroupFactory.class);
        EuroCoinCollectionGroupSqliteRepository repository = new EuroCoinCollectionGroupSqliteRepository(dataSource, tableName, groupFactory);

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
}
