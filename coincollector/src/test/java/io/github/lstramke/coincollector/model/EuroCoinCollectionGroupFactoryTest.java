package io.github.lstramke.coincollector.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EuroCoinCollectionGroupFactoryTest {

	private record FromDbEntryTestcase(
		String id,
		String name,
		String ownerId,
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
			new FromDbEntryTestcase("g-1", "My Group", "u-1", false, null,
				"valid: proper row creates group with preserved id"),
			new FromDbEntryTestcase(null, "Name", "u-1", false, SQLException.class,
				"invalid: null id -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("   ", "Name", "u-1", false, SQLException.class,
				"invalid: blank id -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("g-2", "Name", null, false, SQLException.class,
				"invalid: null ownerId -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("g-3", "Name", "", false, SQLException.class,
				"invalid: blank ownerId -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("ignored", "ignored", "ignored", true, SQLException.class,
				"sql error: ResultSet#getString throws SQLException which is propagated")
		);
	}

	@ParameterizedTest(name = "{index} - {0}")
	@MethodSource("fromDbEntryTestcases")
	void testFromDataBaseEntry(FromDbEntryTestcase testcase) throws Exception {
		EuroCoinCollectionGroupFactory factory = new EuroCoinCollectionGroupFactory();

		ResultSet rs = mock(ResultSet.class);

		if (testcase.sqlExceptionOnAccess){
			when(rs.getString(anyString())).thenThrow(new SQLException("rs boom"));
		} else {
			when(rs.getString("group_id")).thenReturn(testcase.id);
			when(rs.getString("name")).thenReturn(testcase.name);
			when(rs.getString("owner_id")).thenReturn(testcase.ownerId);
		}

		if (testcase.expectedException != null){
			assertThrows(testcase.expectedException, () -> 
				factory.fromDataBaseEntry(rs),
				"Expected exception was not thrown for: " + testcase.description
			);
		} else {
			EuroCoinCollectionGroup group = factory.fromDataBaseEntry(rs);
			assertNotNull(group, "group must be created for valid input");
			assertEquals(testcase.id, group.getId());
			assertEquals(testcase.name, group.getName());
			assertEquals(testcase.ownerId, group.getOwnerId());
			assertEquals(0, group.getTotalCollections(), "new group from DB should have no collections initially");

			verify(rs, times(1)).getString("group_id");
			verify(rs, times(1)).getString("name");
			verify(rs, times(1)).getString("owner_id");
			verifyNoMoreInteractions(rs);
		}
	}
}
