package io.github.lstramke.coincollector.repositories.sqlite;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.EuroCoinFactory;
import io.github.lstramke.coincollector.model.Mint;

@ExtendWith(MockitoExtension.class)
class EuroCoinSqliteRepositoryTest {

    private static final String tableName = "test_coins";
    private static final EuroCoin dummyCoin = new EuroCoinBuilder()
                                    .setValue(CoinValue.TWO_EUROS)
                                    .setYear(2002)
                                    .setMintCountry(CoinCountry.GERMANY)
                                    .setMint(Mint.BERLIN)
                                    .setCollectionId("dummy collection")
                                    .build();

    private record CreateTestcase(
        EuroCoin coin,
        Connection connection,
        boolean shouldThrowSQLException,
        int rowsAffected,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<CreateTestcase> createTestcases(){
        return Stream.of(
            new CreateTestcase(dummyCoin, mock(Connection.class), false, 1, null, "Valid Coin - successful insert"),
            new CreateTestcase(dummyCoin, mock(Connection.class), false, 0, SQLException.class, "Valid Coin - unsuccessful insert"),
            new CreateTestcase(null, mock(Connection.class), false, 1, IllegalArgumentException.class, "Null Coin"),
            new CreateTestcase(dummyCoin, null, false, 1, IllegalArgumentException.class, "Null Connection"),
            new CreateTestcase(dummyCoin, mock(Connection.class), true, 1, SQLException.class, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(tableName, euroCoinFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try{
            if(testcase.coin != null && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.create(testcase.connection, testcase.coin),
                    "Expected exception was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.create(testcase.connection, testcase.coin),
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
        Optional<EuroCoin> expectedEuroCoin,
        Class<? extends Exception> expectedException,
        String description

    ){
        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ReadTestcase> readTestcases(){
        return Stream.of(
            new ReadTestcase(null, mock(Connection.class), false, false, false, Optional.empty(), IllegalArgumentException.class, "Null id"),
            new ReadTestcase("validId", null, false, false, false, Optional.empty(), IllegalArgumentException.class, "Null connection"),
            new ReadTestcase("",mock(Connection.class), false, false, false, Optional.empty(), IllegalArgumentException.class, "Empty id"),
            new ReadTestcase("validId",mock(Connection.class), false, true, false, Optional.of(dummyCoin), null, "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId",mock(Connection.class), false, true, true, Optional.empty(), null, "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId",mock(Connection.class), true, true, false, Optional.empty(), SQLException.class, "SQLException during read attempt"),
            new ReadTestcase("validId",mock(Connection.class), false, false, false, Optional.empty(), null, "Valid id -  no read hit")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase){
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(tableName, euroCoinFactory);

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
                            when(euroCoinFactory.fromDataBaseEntry(resultSet))
                                .thenThrow(new SQLException("Factory error"));
                        }
                        else if(testcase.expectedEuroCoin.isPresent()){
                            when(euroCoinFactory.fromDataBaseEntry(resultSet))
                                .thenReturn(testcase.expectedEuroCoin.get());
                        }
                    }
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException,
                    () -> repository.read(testcase.connection, testcase.id),
                    "Expected Exception was not thrown for: " + testcase.description
                );
            } else {
                Optional<EuroCoin> result = repository.read(testcase.connection, testcase.id);
                assertEquals(testcase.expectedEuroCoin, result,
                    "Result value mismatch for: " + testcase.description);

                if (testcase.id != null && !testcase.id.isBlank()) {
                    verify(testcase.connection).prepareStatement(anyString());
                    verify(preparedStatement).setString(1, testcase.id);
                    verify(preparedStatement).executeQuery();

                    if (testcase.hitInDB) {
                        verify(resultSet).next();
                        if (!testcase.factoryThrowsSQLException) {
                            verify(euroCoinFactory).fromDataBaseEntry(resultSet);
                        }
                    }
                }
            }
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateTestcase(
        EuroCoin coin,
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
            new UpdateTestcase(dummyCoin, mock(Connection.class), false, 1, null, "Valid coin - successful update"),
            new UpdateTestcase(dummyCoin, mock(Connection.class), false, 0, SQLException.class, "Valid coin - unsuccessful update"),
            new UpdateTestcase(dummyCoin, mock(Connection.class), true, 0, SQLException.class, "SQLException during update attempt"),
            new UpdateTestcase(null, mock(Connection.class), false, 0, IllegalArgumentException.class, "Null coin"),
            new UpdateTestcase(dummyCoin, null, false, 0, IllegalArgumentException.class, "Null connection")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(tableName, euroCoinFactory);

        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.coin != null && testcase.connection != null){
                when(testcase.connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () ->
                    repository.update(testcase.connection, testcase.coin),
                    "Expected SQLException was not thrown for: " + testcase.description
                );
            } else {
                assertDoesNotThrow(() ->
                    repository.update(testcase.connection, testcase.coin),
                    "Unexpected exception thrown for: " + testcase.description
                );

                verify(testcase.connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
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
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(tableName, euroCoinFactory);

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
        List<EuroCoin> coinsInDB,
        int factoryThrowsOnRow,
        List<EuroCoin> expectedEuroCoins,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllTestcase> getAllTestcases(){
        EuroCoin dummyCoin2 = new EuroCoinBuilder()
            .setValue(CoinValue.TWO_EUROS)
            .setYear(2004)
            .setMintCountry(CoinCountry.FRANCE)
            .setCollectionId("dummy collection2")
            .build();
        return Stream.of(
            new GetAllTestcase(null, true, List.of(), -1, List.of(), IllegalArgumentException.class, "Null connection"),
            new GetAllTestcase(mock(Connection.class), true, List.of(), -1, List.of(), SQLException.class, "SQLException during select all attempt"),
            new GetAllTestcase(mock(Connection.class), false, List.of(), -1, List.of(), null, "Empty ResultSet"),
            new GetAllTestcase(mock(Connection.class), false, List.of(dummyCoin), -1, List.of(dummyCoin), null, "Single coin"),
            new GetAllTestcase(mock(Connection.class), false, List.of(dummyCoin, dummyCoin2), -1, List.of(dummyCoin, dummyCoin2), null, "Multiple coin - all valid"),
            new GetAllTestcase(mock(Connection.class), false, List.of(dummyCoin, dummyCoin2), 1, List.of(dummyCoin), null, "Multiple coin - with factory exception")
        );
    }


    @ParameterizedTest(name = " {index} - {0}")
    @MethodSource("getAllTestcases")
    void testGetAll(GetAllTestcase testcase){
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(tableName, euroCoinFactory);

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
                    lenient().when(resultSet.next()).then(hasNext -> row.incrementAndGet() < testcase.coinsInDB.size());
                    lenient().when(resultSet.getString(eq("coin_id"))).then(coinId -> testcase.coinsInDB.get(row.get()).getId());
                    lenient().when(euroCoinFactory.fromDataBaseEntry(resultSet)).then(coin -> {
                        int i = row.get();
                        if (testcase.factoryThrowsOnRow == i){
                            throw new SQLException("factory exception");
                        } else {
                            return testcase.coinsInDB.get(i);
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
                List<EuroCoin> result = repository.getAll(testcase.connection);
                assertEquals(testcase.expectedEuroCoins, result,
                    "Result value mismatch for: " + testcase.description
                );

                verify(testcase.connection).prepareStatement(anyString());
                verify(preparedStatement).executeQuery();

                if (!testcase.coinsInDB.isEmpty()) {
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
        String description) {

        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ExistsTestcase> existsTestcases() {
        return Stream.of(
            new ExistsTestcase("valid-id", mock(Connection.class), true, true, null, false, "Coin exists"),
            new ExistsTestcase("non-existing-id", mock(Connection.class), false, false,null, false, "Coin does not exist"),
            new ExistsTestcase(null, mock(Connection.class), false, false, IllegalArgumentException.class, false, "Null ID"),
            new ExistsTestcase("validId", null, false, false, IllegalArgumentException.class, false, "Null Connection"),
            new ExistsTestcase("", mock(Connection.class), false, false, IllegalArgumentException.class, false, "Empty ID"),
            new ExistsTestcase("  ", mock(Connection.class), false, false, IllegalArgumentException.class, false, "Whitespace-only ID"),
            new ExistsTestcase("db-error-id", mock(Connection.class), false, false, SQLException.class, true, "Database error")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestcases")
    void testExists(ExistsTestcase testcase) {
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(tableName, euroCoinFactory);

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
        String coinId, 
        Integer year, 
        CoinValue coinValue,
        CoinCountry mintCountry, 
        Mint mint, 
        String collectionId, 
        boolean expectedResult,
        boolean isNullCoin,
        String description) {

        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<ValidationTestcase> validateEuroCoinTestcases() {
        CoinValue mockCoinValue = mock(CoinValue.class);
        CoinCountry mockMintCountry = CoinCountry.GERMANY;
        Mint mockMint = mock(Mint.class);

        return Stream.of(
            new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", true, false, "Valid coin"),
            new ValidationTestcase(null, null, null, null, null, null, false, true, "Null coin"),
            new ValidationTestcase(null, 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, false, "Coin with null ID"),
            new ValidationTestcase("", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, false, "Coin with empty ID"),
            new ValidationTestcase("   ", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, false, "Coin with whitespace-only ID"),
            new ValidationTestcase("valid-id", 1998, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, false, "Coin with year before 1999"),
            new ValidationTestcase("valid-id", 2020, null, mockMintCountry, mockMint, "collection-123", false, false, "Coin with null CoinValue"),
            new ValidationTestcase("valid-id", 2020, mockCoinValue, null, mockMint, "collection-123", false, false, "Coin with null MintCountry"),
            new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, null, "collection-123", false, false, "German Coin with null Mint"),
            new ValidationTestcase("valid-id", 2020, mockCoinValue, CoinCountry.FRANCE, null, "collection-123", true, false, "Not German Coin with null Mint"),
            new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, null, false, false, "Coin with null CollectionId"),
            new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, "", false, false, "Coin with empty CollectionId")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validateEuroCoinTestcases")
    void testValidateEuroCoin(ValidationTestcase testcase) {
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(tableName, euroCoinFactory);

        EuroCoin coin = null;
        if (!testcase.isNullCoin) {
            coin = mock(EuroCoin.class);
            lenient().when(coin.getId()).thenReturn(testcase.coinId());
            lenient().when(coin.getYear()).thenReturn(testcase.year());
            lenient().when(coin.getValue()).thenReturn(testcase.coinValue());
            lenient().when(coin.getMintCountry()).thenReturn(testcase.mintCountry());
            lenient().when(coin.getMint()).thenReturn(testcase.mint());
            lenient().when(coin.getCollectionId()).thenReturn(testcase.collectionId());
        }

        boolean result = repository.validateEuroCoin(coin);

        assertEquals(testcase.expectedResult(), result,
                "Validation result mismatch for: " + testcase.description);
    }
}
