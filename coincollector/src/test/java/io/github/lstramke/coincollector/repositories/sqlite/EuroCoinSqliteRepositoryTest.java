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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinFactory;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.Mint;

@ExtendWith(MockitoExtension.class)
class EuroCoinSqliteRepositoryTest {

    @Mock
    private Connection connection;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private ResultSet resultSet;
    
    @Mock
    private EuroCoinFactory euroCoinFactory;
    
    @Mock
    private EuroCoin euroCoin;
    
    private EuroCoinSqliteRepository repository;
    private final String tableName = "test_coins";
    
    @BeforeEach
    void setUp() {
        repository = new EuroCoinSqliteRepository(connection, tableName, euroCoinFactory);
    }
    
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("existsTestCases")
    void testExists(ExistsTestCase testCase) {
        System.out.println("Testing: " + testCase.description());

        reset(connection, preparedStatement, resultSet);

        try {
            if (testCase.coinId() != null && !testCase.coinId().trim().isEmpty()) {
                when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
                if (testCase.shouldThrowSQLException()) {
                    when(preparedStatement.executeQuery()).thenThrow(new SQLException("Database connection failed"));
                } else {
                    when(preparedStatement.executeQuery()).thenReturn(resultSet);
                    when(resultSet.next()).thenReturn(testCase.resultSetHasNext());
                }
            }
            
            Optional<Boolean> result = repository.exists(testCase.coinId());

            assertEquals(testCase.expectedOptionalIsPresent(), result.isPresent(), 
                "Optional presence mismatch for: " + testCase.description());

            if (result.isPresent()) {
                assertEquals(testCase.expectedResult(), result.get(), 
                    "Result value mismatch for: " + testCase.description());
            }

            if (testCase.coinId() != null && !testCase.coinId().trim().isEmpty() && !testCase.shouldThrowSQLException()) {
                verify(connection).prepareStatement(anyString());
                verify(preparedStatement).setString(1, testCase.coinId());
                verify(preparedStatement).executeQuery();
                verify(resultSet).next();
            }

        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    record ExistsTestCase(
        String coinId, 
        boolean resultSetHasNext, 
        boolean expectedResult, 
        boolean expectedOptionalIsPresent, 
        boolean shouldThrowSQLException,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }
    
    static Stream<ExistsTestCase> existsTestCases() {
        return Stream.of(
            new ExistsTestCase("valid-id", true, true, true, false, "Coin exists"),
            new ExistsTestCase("non-existing-id", false, false, true, false, "Coin does not exist"),
            new ExistsTestCase(null, false, false, true, false, "Null ID"),
            new ExistsTestCase("", false, false, true, false, "Empty ID"),
            new ExistsTestCase("  ", false, false, true, false, "Whitespace-only ID"),
            new ExistsTestCase("db-error-id", false, false, false, true, "Database error")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("validateEuroCoinTestCases")
    void testValidateEuroCoin(ValidationTestCase testCase) {
        System.out.println("Testing: " + testCase.testDescription());
        
        EuroCoin coin = null;
        if (!testCase.testDescription().contains("Null coin")) {
            coin = mock(EuroCoin.class);
            lenient().when(coin.getId()).thenReturn(testCase.coinId());
            lenient().when(coin.getYear()).thenReturn(testCase.year());
            lenient().when(coin.getValue()).thenReturn(testCase.coinValue());
            lenient().when(coin.getMintCountry()).thenReturn(testCase.mintCountry());
            lenient().when(coin.getMint()).thenReturn(testCase.mint());
            lenient().when(coin.getCollectionId()).thenReturn(testCase.collectionId());
        }
    
        boolean result = repository.validateEuroCoin(coin);
        
        assertEquals(testCase.expectedResult(), result, 
            "Validation result mismatch for: " + testCase.testDescription());
    }

    record ValidationTestCase(
        String coinId,
        Integer year,
        CoinValue coinValue,
        CoinCountry mintCountry,
        Mint mint,
        String collectionId,
        boolean expectedResult,
        String testDescription
    ) {
        @Override
        public String toString() {
            return testDescription;
        }
    }
    
    static Stream<ValidationTestCase> validateEuroCoinTestCases() {
        CoinValue mockCoinValue = mock(CoinValue.class);
        CoinCountry mockMintCountry = mock(CoinCountry.class);
        Mint mockMint = mock(Mint.class);
        
        return Stream.of(
            new ValidationTestCase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", true, "Valid coin"),
            new ValidationTestCase(null, null, null, null, null, null, false, "Null coin"),
            new ValidationTestCase(null, 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with null ID"),
            new ValidationTestCase("", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with empty ID"),
            new ValidationTestCase("   ", 2020, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with whitespace-only ID"),
            new ValidationTestCase("valid-id", 1998, mockCoinValue, mockMintCountry, mockMint, "collection-123", false, "Coin with year before 1999"),
            new ValidationTestCase("valid-id", 2020, null, mockMintCountry, mockMint, "collection-123", false, "Coin with null CoinValue"),
            new ValidationTestCase("valid-id", 2020, mockCoinValue, null, mockMint, "collection-123", false, "Coin with null MintCountry"),
            new ValidationTestCase("valid-id", 2020, mockCoinValue, mockMintCountry, null, "collection-123", false, "Coin with null Mint"),
            new ValidationTestCase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, null, false, "Coin with null CollectionId"),
            new ValidationTestCase("valid-id", 2020, mockCoinValue, mockMintCountry, mockMint, "", false, "Coin with empty CollectionId")
        );
    }
}
