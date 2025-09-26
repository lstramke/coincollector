package io.github.lstramke.coincollector.model;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

public class CoinValueTest {
	private record FromCentValueTestcase(
		int centValue,
		CoinValue expectedCoinValue,
		Class<? extends Exception> expectedException,
		String description
	){
		@Override
		public String toString(){
			return description;
		}
	}

	private static Stream<FromCentValueTestcase> fromCentValueTestcases(){
		Stream<FromCentValueTestcase> validCases = Stream
			.of(CoinValue.values())
			.map(value -> new FromCentValueTestcase(
				value.getCentValue(),
				value,
				null,
				"valid: " + value.getCentValue() + " -> " + value.name()
			));

		Stream<FromCentValueTestcase> errorCases = Stream.of(
			new FromCentValueTestcase(-1, null, IllegalArgumentException.class, "invalid: negative cent value -1"),
			new FromCentValueTestcase(3, null, IllegalArgumentException.class, "invalid: non-existent 3 cent")
		);

		return Stream.concat(validCases, errorCases);
	}

	@ParameterizedTest(name = "{index} - {0}")
	@MethodSource("fromCentValueTestcases")
	void testFromCentValue(FromCentValueTestcase testcase){
		if (testcase.expectedException != null){
			assertThrows(testcase.expectedException, () -> 
                CoinValue.fromCentValue(testcase.centValue),
				"Expected exception was not thrown for: " + testcase.description
			);
		} else {
			CoinValue actual = CoinValue.fromCentValue(testcase.centValue);
			assertEquals(testcase.expectedCoinValue, actual,
				"Result value mismatch for: " + testcase.description
			);
		}
	}
}
