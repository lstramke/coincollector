package io.github.lstramke.coincollector.model;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

public class CoinCountryTest {
    
    private record FromIsoCodeTestcase(
        String isoCode,
        CoinCountry expectedCoinCountry,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<FromIsoCodeTestcase> fromIsoCodeTestcases(){
        Stream<FromIsoCodeTestcase> validCases = Stream
            .of(CoinCountry.values())
            .map(country -> new FromIsoCodeTestcase(
                country.getIsoCode(),
                country,
                null,
                "valid: " + country.getIsoCode() + " -> " + country.name()
            ));

        Stream<FromIsoCodeTestcase> errorCases = Stream.of(
            new FromIsoCodeTestcase("XX", null, IllegalArgumentException.class, "invalid: unknown code 'XX'"),
            new FromIsoCodeTestcase(null, null, IllegalArgumentException.class, "invalid: null code")
        );

        return Stream.concat(validCases, errorCases);
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("fromIsoCodeTestcases")
    void testFromIsoCode(FromIsoCodeTestcase testcase){
        if (testcase.expectedException != null){
            assertThrows(testcase.expectedException, () -> 
                CoinCountry.fromIsoCode(testcase.isoCode),
                "Expected exception was not thrown for: " + testcase.description
            );
        } else {
            CoinCountry actual = CoinCountry.fromIsoCode(testcase.isoCode);
            assertEquals(testcase.expectedCoinCountry, actual,
                "Result value mismatch for: " + testcase.description
            );
        }
    }
}
