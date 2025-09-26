package io.github.lstramke.coincollector.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EuroCoinCollectionFactoryTest {

	private record FromDbEntryTestcase(
		String id,
		String name,
		String groupId,
		boolean sqlExceptionOnAccess,
		Class<? extends Exception> expectedException,
		String description
	){
		@Override
		public String toString(){
			return description;
		}
	}

	private static Stream<FromDbEntryTestcase> fromDbEntryTestcases(){
		return Stream.of(
			new FromDbEntryTestcase("c-1", "My Collection", "g-1", false, null,
				"valid: proper row creates collection with preserved id"),
			new FromDbEntryTestcase(null, "Name", "g-1", false, SQLException.class,
				"invalid: null id -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("   ", "Name", "g-1", false, SQLException.class,
				"invalid: blank id -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("c-2", "Name", null, false, SQLException.class,
				"invalid: null groupId -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("c-3", "Name", "", false, SQLException.class,
				"invalid: blank groupId -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("ignored", "ignored", "ignored", true, SQLException.class,
				"sql error: ResultSet#getString throws SQLException which is propagated")
		);
	}

	@ParameterizedTest(name = "{index} - {0}")
	@MethodSource("fromDbEntryTestcases")
	void testFromDataBaseEntry(FromDbEntryTestcase testcase) throws Exception {
        EuroCoinCollectionFactory factory = new EuroCoinCollectionFactory();

		ResultSet rs = mock(ResultSet.class);

		if (testcase.sqlExceptionOnAccess){
			when(rs.getString(anyString())).thenThrow(new SQLException("rs boom"));
		} else {
			when(rs.getString("collection_id")).thenReturn(testcase.id);
			when(rs.getString("name")).thenReturn(testcase.name);
			when(rs.getString("group_id")).thenReturn(testcase.groupId);
		}

		if (testcase.expectedException != null){
			assertThrows(testcase.expectedException, () -> 
                factory.fromDataBaseEntry(rs),
				"Expected exception was not thrown for: " + testcase.description
            );
        } else {
			EuroCoinCollection col = factory.fromDataBaseEntry(rs);
			assertNotNull(col, "collection must be created for valid input");
			assertEquals(testcase.id, col.getId());
			assertEquals(testcase.name, col.getName());
			assertEquals(testcase.groupId, col.getGroupId());
			assertEquals(0, col.getCoinCount(), "new collection from DB should have no coins initially");

			verify(rs, times(1)).getString("collection_id");
			verify(rs, times(1)).getString("name");
			verify(rs, times(1)).getString("group_id");
			verifyNoMoreInteractions(rs);
		}
	}
}
