package io.github.lstramke.coincollector.model;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CoinDescriptionTest {
    
    private record AutoDescriptionGERTestcase(
        CoinValue value, 
        int year, 
        CoinCountry country, 
        Mint mint,
        String expectedString,
        Class<? extends Exception> expectedException,
        String description
        
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<AutoDescriptionGERTestcase> autoDescriptionGERTestcases(){
        return Stream.of(
            new AutoDescriptionGERTestcase(null, 2020, CoinCountry.GERMANY, Mint.BERLIN, 
                null, IllegalArgumentException.class, "null CoinValue -> IllegalArgumentException"),
            new AutoDescriptionGERTestcase(CoinValue.ONE_EURO, -1, CoinCountry.GERMANY, Mint.BERLIN, 
                null, IllegalArgumentException.class, "null CoinValue -> IllegalArgumentException"),
            new AutoDescriptionGERTestcase(CoinValue.ONE_EURO, 2020, null, Mint.BERLIN, 
                null, IllegalArgumentException.class, "null CoinValue -> IllegalArgumentException"),
            new AutoDescriptionGERTestcase(CoinValue.TWO_EUROS, 2024, CoinCountry.GERMANY, Mint.BERLIN, 
                "2 Euro Münze aus Deutschland aus dem Jahr 2024 aus der Prägestätte A", null, "validFall - with mint BERLIN"),
            new AutoDescriptionGERTestcase(CoinValue.TWO_EUROS, 2024, CoinCountry.ITALY, null, 
                "2 Euro Münze aus Italien aus dem Jahr 2024", null, "valid - without mint")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("autoDescriptionGERTestcases")
    void testCoinDescription_GER(AutoDescriptionGERTestcase testcase){
        if (testcase.expectedException != null){
            Assertions.assertThrows(testcase.expectedException, () -> 
                new CoinDescription(testcase.value, testcase.year, testcase.country, testcase.mint),
                "Expected exception was not thrown for: " + testcase.description
            );
        } else {
            CoinDescription desc = new CoinDescription(testcase.value, testcase.year, testcase.country, testcase.mint);
            Assertions.assertEquals(testcase.expectedString, desc.toString(),
                "Result value mismatch for: " + testcase.description);
        }
    }
}
