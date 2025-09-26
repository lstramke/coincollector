package io.github.lstramke.coincollector.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MintTest {
    
    private record FromMintMarkTestcase(
        String mintMark,
        Mint expectedMint,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<FromMintMarkTestcase> fromMintMarkTestcases(){
       Stream<FromMintMarkTestcase> validCases = Stream
			.of(Mint.values())
			.map(value -> new FromMintMarkTestcase(
				value.getMintMark(),
				value,
				null,
				"valid: " + value.getMintMark() + " -> " + value.name()
			));

		Stream<FromMintMarkTestcase> errorCases = Stream.of(
			new FromMintMarkTestcase("", null, IllegalArgumentException.class, "invalid: empty mintMark"),
			new FromMintMarkTestcase(null, null, IllegalArgumentException.class, "invalid: null mintMark")
		);

		return Stream.concat(validCases, errorCases);
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("fromMintMarkTestcases")
    void testFromMintMark(FromMintMarkTestcase testcase){
        if (testcase.expectedException != null){
			assertThrows(testcase.expectedException, () -> 
                Mint.fromMintMark(testcase.mintMark),
				"Expected exception was not thrown for: " + testcase.description
			);
		} else {
			Mint actual = Mint.fromMintMark(testcase.mintMark);
			assertEquals(testcase.expectedMint, actual,
				"Result value mismatch for: " + testcase.description
			);
		}
    }
}
