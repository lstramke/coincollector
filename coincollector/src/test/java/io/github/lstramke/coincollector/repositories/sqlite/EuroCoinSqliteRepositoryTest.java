package io.github.lstramke.coincollector.repositories.sqlite;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.sql.DataSource;
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

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("createTestcases")
    void testCreate(CreateTestcase testcase){
        System.out.println("Testing: " + testcase.description());

        DataSource dataSource = mock(DataSource.class);
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(dataSource, tableName, euroCoinFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try{
            if(testcase.coin != null){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Insert failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            boolean result = repository.create(testcase.coin());

            assertEquals(testcase.expectedResult(), result);

            if (testcase.coin != null && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }

        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    record CreateTestcase(
        EuroCoin coin,
        boolean shouldThrowSQLException,
        int rowsAffected,
        boolean expectedResult,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    static Stream<CreateTestcase> createTestcases(){
        return Stream.of(
            new CreateTestcase(dummyCoin, false, 1, true, "Valid Coin - successful insert"),
            new CreateTestcase(dummyCoin, false, 0, false, "Valid Coin - unsuccessful insert"),
            new CreateTestcase(null, false, 1, false, "Null Coin"),
            new CreateTestcase(dummyCoin, true, 1, false, "SQLException during create attempt")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("readTestcases")
    void testRead(ReadTestcase testcase){
        System.out.println("Testing: " + testcase.description());

        DataSource dataSource = mock(DataSource.class);
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(dataSource, tableName, euroCoinFactory);

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
                            when(euroCoinFactory.fromDataBaseEntry(resultSet))
                                .thenThrow(new SQLException("Factory error"));
                        }
                        else if(testcase.expectedEuroCoin.isPresent()){
                            when(euroCoinFactory.fromDataBaseEntry(resultSet))
                                .thenReturn(testcase.expectedEuroCoin.get());
                        }
                    }
                }

                Optional<EuroCoin> result = repository.read(testcase.id);

                assertEquals(testcase.expectedEuroCoin, result);

                if (testcase.id != null && !testcase.id.isBlank() && !testcase.shouldThrowSQLException && testcase.hitInDB) {
                    verify(dataSource).getConnection();
                    verify(connection).prepareStatement(anyString());
                    verify(preparedStatement).setString(1, testcase.id);
                    verify(preparedStatement).executeQuery();
                    verify(resultSet).next();
                    if (!testcase.factoryThrowsSQLException) {
                        verify(euroCoinFactory).fromDataBaseEntry(resultSet);
                    }
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
        Optional<EuroCoin> expectedEuroCoin,
        String description

    ){
        @Override
        public String toString() {
            return description;
        }
    }

    static Stream<ReadTestcase> readTestcases(){
        return Stream.of(
            new ReadTestcase(null, false, false, false, Optional.empty(), "Null id"),
            new ReadTestcase("", false, false, false, Optional.empty(), "Empty id"),
            new ReadTestcase("validId", false, true, false, Optional.of(dummyCoin), "Valid id - read hit, no SQLException"),
            new ReadTestcase("validId", false, true, true, Optional.empty(), "Valid id - read hit, SQLException from factory"),
            new ReadTestcase("validId", true, true, false, Optional.empty(), "SQLException during read attempt"),
            new ReadTestcase("validId", false, false, false, Optional.empty(), "Valid id -  no read hit")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateTestcases")
    void testUpdate(UpdateTestcase testcase){
        System.out.println("Testing: " + testcase.description());

        DataSource dataSource = mock(DataSource.class);
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(dataSource, tableName, euroCoinFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);

        try {
            if(testcase.coin != null){
                when(dataSource.getConnection()).thenReturn(connection);
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

                if (testcase.shouldThrowSQLException()) {
                    when(preparedStatement.executeUpdate()).thenThrow(new SQLException("Update failed"));
                } else {
                    when(preparedStatement.executeUpdate()).thenReturn(testcase.rowsAffected);
                }
            }

            boolean result = repository.update(testcase.coin);

            assertEquals(testcase.expectedResult, result);

            if (testcase.coin != null && !testcase.shouldThrowSQLException()) {
                verify(dataSource).getConnection();
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).executeUpdate();
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }

    }

    record UpdateTestcase(
        EuroCoin coin,
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

    static Stream<UpdateTestcase> updateTestcases(){
        return Stream.of(
            new UpdateTestcase(dummyCoin, false, 1, true, "Valid coin - successful update"),
            new UpdateTestcase(dummyCoin, false, 0, false, "Valid coin - unsuccessful update"),
            new UpdateTestcase(dummyCoin, true, 0, false, "SQLException during update attempt"),
            new UpdateTestcase(null, false, 0, false, "Null coin")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testDelete(DeleteTestcase testcase){
        System.out.println("Testing: " + testcase.description());

        DataSource dataSource = mock(DataSource.class);
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(dataSource, tableName, euroCoinFactory);

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

            assertEquals(testcase.expectedResult, result);

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

    static Stream<DeleteTestcase> deleteTestcases(){
        return Stream.of(
            new DeleteTestcase(null, false, 0, false, "Null Id"),
            new DeleteTestcase("", false, 0, false, "Empty Id"),
            new DeleteTestcase("validId", false, 1, true, "Valid Id - successful delete"),
            new DeleteTestcase("validId", false, 0, false, "Valid Id - unsuccessful delete"),
            new DeleteTestcase("validId", true, 0, false, "SQLException during delete attempt")
        );
    }

    @ParameterizedTest(name = " {index} - {0}")
    @MethodSource("getAllTestcases")
    void testGetAll(GetAllTestcase testcase){
        System.out.println("Testing: " + testcase.description());

        DataSource dataSource = mock(DataSource.class);
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(dataSource, tableName, euroCoinFactory);

        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        try {
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

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

            List<EuroCoin> result = repository.getAll();

            assertEquals(testcase.expectedEuroCoins, result);
  
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    record GetAllTestcase(
        boolean shouldThrowSQLException,
        List<EuroCoin> coinsInDB,
        int factoryThrowsOnRow,
        List<EuroCoin> expectedEuroCoins,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    static Stream<GetAllTestcase> getAllTestcases(){
        EuroCoin dummyCoin2 = new EuroCoinBuilder()
            .setValue(CoinValue.TWO_EUROS)
            .setYear(2004)
            .setMintCountry(CoinCountry.FRANCE)
            .setCollectionId("dummy collection2")
            .build();
        return Stream.of(
            new GetAllTestcase(true, List.of(), -1, List.of(), "SQLException during select all attempt"),
            new GetAllTestcase(false, List.of(), -1, List.of(), "Empty ResultSet"),
            new GetAllTestcase(false, List.of(dummyCoin), -1, List.of(dummyCoin), "Single coin"),
            new GetAllTestcase(false, List.of(dummyCoin, dummyCoin2), -1, List.of(dummyCoin, dummyCoin2), "Multiple coin - all valid"),
            new GetAllTestcase(false, List.of(dummyCoin, dummyCoin2), 1, List.of(dummyCoin), "Multiple coin - with factory exception")
            );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestcases")
    void testExists(ExistsTestcase testcase) {
        System.out.println("Testing: " + testcase.description());

        DataSource dataSource = mock(DataSource.class);
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(dataSource, tableName, euroCoinFactory);

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

            assertEquals(testcase.expectedOptionalIsPresent(), result.isPresent(), 
                        "Optional presence mismatch for: " + testcase.description());

            if (result.isPresent()) {
                assertEquals(testcase.expectedResult(), result.get(),
                        "Result value mismatch for: " + testcase.description());
            }

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

    record ExistsTestcase(
        String coinId, 
        boolean resultSetHasNext, 
        boolean expectedResult,
        boolean expectedOptionalIsPresent,
        boolean shouldThrowSQLException,
        String description) {

        @Override
        public String toString() {
            return description;
        }
    }

    static Stream<ExistsTestcase> existsTestcases() {
        return Stream.of(new ExistsTestcase("valid-id", true, true, true, false, "Coin exists"),
                new ExistsTestcase("non-existing-id", false, false, true, false, "Coin does not exist"),
                new ExistsTestcase(null, false, false, true, false, "Null ID"),
                new ExistsTestcase("", false, false, true, false, "Empty ID"),
                new ExistsTestcase("  ", false, false, true, false, "Whitespace-only ID"),
                new ExistsTestcase("db-error-id", false, false, false, true, "Database error"));
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validateEuroCoinTestcases")
    void testValidateEuroCoin(ValidationTestcase testcase) {
        System.out.println("Testing: " + testcase.testDescription());

        DataSource dataSource = mock(DataSource.class);
        EuroCoinFactory euroCoinFactory = mock(EuroCoinFactory.class);
        EuroCoinSqliteRepository repository = new EuroCoinSqliteRepository(dataSource, tableName,
                euroCoinFactory);

        EuroCoin coin = null;
        if (!testcase.testDescription().contains("Null coin")) {
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
                "Validation result mismatch for: " + testcase.testDescription());
    }

    record ValidationTestcase(
        String coinId, 
        Integer year, 
        CoinValue coinValue,
        CoinCountry mintCountry, 
        Mint mint, 
        String collectionId, 
        boolean expectedResult,
        String testDescription) {

        @Override
        public String toString() {
            return testDescription;
        }
    }

    static Stream<ValidationTestcase> validateEuroCoinTestcases() {
        CoinValue mockCoinValue = mock(CoinValue.class);
        CoinCountry mockMintCountry = mock(CoinCountry.class);
        Mint mockMint = mock(Mint.class);

        return Stream.of(
                new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", true, "Valid coin"),
                new ValidationTestcase(null, null, null, null, null, null, false, "Null coin"),
                new ValidationTestcase(null, 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with null ID"),
                new ValidationTestcase("", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with empty ID"),
                new ValidationTestcase("   ", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with whitespace-only ID"),
                new ValidationTestcase("valid-id", 1998, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with year before 1999"),
                new ValidationTestcase("valid-id", 2020, null, mockMintCountry, mockMint, "collection-123", false, "Coin with null CoinValue"),
                new ValidationTestcase("valid-id", 2020, mockCoinValue, null, mockMint, "collection-123", false, "Coin with null MintCountry"),
                new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, null, "collection-123", false, "Coin with null Mint"),
                new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, null, false, "Coin with null CollectionId"),
                new ValidationTestcase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, "", false, "Coin with empty CollectionId"));
    }
}
