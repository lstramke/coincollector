package io.github.lstramke.coincollector.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EuroCoinFactoryTest {

	private record FromDbEntryTestcase(
		String id,
		int year,
		int centValue,
		String countryIso,
		String mintMark,
		String description,
		String collectionId,
		boolean sqlExceptionOnAccess,
		String expectedMintMark,
		Class<? extends Exception> expectedException,
		String descriptionText
	){
		@Override
		public String toString(){
			return descriptionText;
		}
	}

	private static Stream<FromDbEntryTestcase> fromDbEntryTestcases(){
		return Stream.of(
			new FromDbEntryTestcase(
				"11111111-1111-1111-1111-111111111111",
				2002,
				100,
				"DE",
				"A",
				"1 Euro Münze",
				"col-1",
				false,
				"A",
				null,
				"valid: proper row creates coin with preserved id"
			),
			new FromDbEntryTestcase(
				null,
				2002,
				100,
				"DE",
				"A",
				"desc",
				"col-1",
				false,
				null,
				SQLException.class,
				"invalid: null id -> cannot parse UUID, expect SQLException"
			),
			new FromDbEntryTestcase(
				"   ",
				2002,
				100,
				"DE",
				"A",
				"desc",
				"col-1",
				false,
				null,
				SQLException.class,
				"invalid: blank id -> cannot parse UUID, expect SQLException"
			),
			new FromDbEntryTestcase(
				"22222222-2222-2222-2222-222222222222",
				1998,
				100,
				"DE",
				"A",
				"desc",
				"col-1",
				false,
				null,
				SQLException.class,
				"invalid: year too small -> wrapped to SQLException with ISE cause"
			),
			new FromDbEntryTestcase(
				"33333333-3333-3333-3333-333333333333",
				2002,
				999,
				"DE",
				"A",
				"desc",
				"col-1",
				false,
				null,
				SQLException.class,
				"invalid: unknown coin value -> wrapped to SQLException with IAE cause"
			),
			new FromDbEntryTestcase(
				"44444444-4444-4444-4444-444444444444",
				2002,
				100,
				"ZZ",
				"A",
				"desc",
				"col-1",
				false,
				null,
				SQLException.class,
				"invalid: unknown country iso -> wrapped to SQLException with IAE cause"
			),
			new FromDbEntryTestcase(
				"55555555-5555-5555-5555-555555555555",
				2002,
				100,
				"DE",
				"Z",
				"desc",
				"col-1",
				false,
				null,
				SQLException.class,
				"invalid: unknown mint mark -> wrapped to SQLException with IAE cause"
			),
			new FromDbEntryTestcase(
				"66666666-6666-6666-6666-666666666666",
				2002,
				100,
				"FR",
				"Z",
				"desc",
				"col-1",
				false,
				"UNKNOWN",
				null,
				"valid: unknown mint mark but not German Coin -> mint mark will be UNKNOWN"
			),
			new FromDbEntryTestcase(
				"77777777-7777-7777-7777-777777777777",
				2002,
				100,
				"DE",
				"A",
				"desc",
				null,
				false,
				null,
				SQLException.class,
				"invalid: null collection id -> wrapped to SQLException with ISE cause"
			),
			new FromDbEntryTestcase(
				"ignored",
				2002,
				100,
				"DE",
				"A",
				"desc",
				"col-1",
				true,
				null,
				SQLException.class,
				"sql error: ResultSet access throws SQLException and is propagated"
			)
			);
	}

	@ParameterizedTest(name = "{index} - {0}")
	@MethodSource("fromDbEntryTestcases")
	void testFromDataBaseEntry(FromDbEntryTestcase testcase) throws Exception {
		EuroCoinFactory factory = new EuroCoinFactory();

		ResultSet rs = mock(ResultSet.class);

		if (testcase.sqlExceptionOnAccess){
			when(rs.getString(anyString())).thenThrow(new SQLException("rs boom"));
			when(rs.getInt(anyString())).thenThrow(new SQLException("rs boom"));
		} else {
			when(rs.getString("coin_id")).thenReturn(testcase.id);
			when(rs.getInt("year")).thenReturn(testcase.year);
			when(rs.getInt("coin_value")).thenReturn(testcase.centValue);
			when(rs.getString("mint_country")).thenReturn(testcase.countryIso);
			when(rs.getString("mint")).thenReturn(testcase.mintMark);
			when(rs.getString("description")).thenReturn(testcase.description);
			when(rs.getString("collection_id")).thenReturn(testcase.collectionId);
		}

		if (testcase.expectedException != null){
			assertThrows(testcase.expectedException, () -> factory.fromDataBaseEntry(rs),
                "Expected exception was not thrown for: " + testcase.description
            );
		} else {
			EuroCoin coin = factory.fromDataBaseEntry(rs);
			assertNotNull(coin);
			if (testcase.id != null && !testcase.id.isBlank()){
				assertEquals(UUID.fromString(testcase.id), coin.getId());
			} else {
				assertNotNull(coin.getId());
			}
			assertEquals(testcase.year, coin.getYear());
			assertEquals(testcase.centValue, coin.getValue().getCentValue());
			assertEquals(testcase.countryIso, coin.getMintCountry().getIsoCode());
			assertEquals(testcase.expectedMintMark, coin.getMint().getMintMark());
			assertEquals(testcase.collectionId, coin.getCollectionId());
			assertEquals(testcase.description, coin.getDescription().toString());

			verify(rs, times(1)).getString("coin_id");
			verify(rs, times(1)).getInt("year");
			verify(rs, times(1)).getInt("coin_value");
			verify(rs, times(1)).getString("mint_country");
			verify(rs, times(1)).getString("mint");
			verify(rs, times(1)).getString("description");
			verify(rs, times(1)).getString("collection_id");
			verifyNoMoreInteractions(rs);
		}
	}
}
