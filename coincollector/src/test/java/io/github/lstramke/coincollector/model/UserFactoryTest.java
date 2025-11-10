package io.github.lstramke.coincollector.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UserFactoryTest {

	private record FromDbEntryTestcase(
		String id,
		String name,
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
			new FromDbEntryTestcase("u-1", "Alice", false, null, "valid: proper row creates user with preserved id"),
			new FromDbEntryTestcase(null, "Bob", false, SQLException.class, "invalid: null id -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("   ", "Bob", false, SQLException.class, "invalid: blank id -> wrapped to SQLException with IAE cause"),
			new FromDbEntryTestcase("ignored", "ignored", true, SQLException.class, "sql error: ResultSet#getString throws SQLException which is propagated")
		);
	}

	@ParameterizedTest(name = "{index} - {0}")
	@MethodSource("fromDbEntryTestcases")
	void testFromDataBaseEntry(FromDbEntryTestcase testcase) throws Exception {
		UserFactory factory = new UserFactory();

		ResultSet rs = mock(ResultSet.class);

		if (testcase.sqlExceptionOnAccess){
			when(rs.getString(anyString())).thenThrow(new SQLException("rs boom"));
		} else {
			when(rs.getString("user_id")).thenReturn(testcase.id);
			when(rs.getString("username")).thenReturn(testcase.name);
		}

		if (testcase.expectedException != null){
			assertThrows(testcase.expectedException, () -> factory.fromDataBaseEntry(rs),
            "Expected exception was not thrown for: " + testcase.description
            );
		} else {
			User user = factory.fromDataBaseEntry(rs);
			assertNotNull(user);
			assertEquals(testcase.id, user.getId());
			assertEquals(testcase.name, user.getName());

			verify(rs, times(1)).getString("user_id");
			verify(rs, times(1)).getString("username");
			verifyNoMoreInteractions(rs);
		}
	}
}
