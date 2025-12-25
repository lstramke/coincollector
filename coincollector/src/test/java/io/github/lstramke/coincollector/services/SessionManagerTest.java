package io.github.lstramke.coincollector.services;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.UUID;
import java.util.stream.Stream;

class SessionManagerTest {

	private record CreateSessionCase(String userId, boolean expectValid, String description) {
		@Override public String toString() { return description; }
	}

	private static Stream<CreateSessionCase> createSessionCases() {
		return Stream.of(
			new CreateSessionCase("user1", true, "Valid userId: session is valid and userId matches")
		);
	}

    @ParameterizedTest(name = "{index} - {0}")
	@MethodSource("createSessionCases")
	void testCreateSession(CreateSessionCase tc) {
		SessionManagerImpl sm = new SessionManagerImpl();
		String sessionId = sm.createSession(tc.userId);
		assertNotNull(sessionId);
		assertDoesNotThrow(() -> UUID.fromString(sessionId));
		assertEquals(tc.expectValid, sm.validateSession(sessionId));
		assertEquals(tc.userId, sm.getUserId(sessionId));
	}

	private record ValidateSessionCase(String sessionId, boolean expected, String description) {
		@Override public String toString() { return description; }
	}

	private static Stream<ValidateSessionCase> validateSessionCases() {
		return Stream.of(
			new ValidateSessionCase("valid", true, "Known sessionId is valid"),
			new ValidateSessionCase(null, false, "Null sessionId is invalid"),
			new ValidateSessionCase("not-in-map", false, "Unknown sessionId is invalid")
		);
	}

    @ParameterizedTest(name = "{index} - {0}")
	@MethodSource("validateSessionCases")
	void testValidateSession(ValidateSessionCase tc) {
		SessionManagerImpl sm = new SessionManagerImpl();
		String validId = sm.createSession("user1");
		String sessionId = tc.sessionId;
		if ("valid".equals(sessionId)) sessionId = validId;
		assertEquals(tc.expected, sm.validateSession(sessionId));
	}

	private record InvalidateSessionCase(String userId, String description) {
		@Override public String toString() { return description; }
	}

	private static Stream<InvalidateSessionCase> invalidateSessionCases() {
		return Stream.of(
			new InvalidateSessionCase("user1", "Session is invalid after invalidation")
		);
	}

    @ParameterizedTest(name = "{index} - {0}")
	@MethodSource("invalidateSessionCases")
	void testInvalidateSession(InvalidateSessionCase tc) {
		SessionManagerImpl sm = new SessionManagerImpl();
		String sessionId = sm.createSession(tc.userId);
		assertTrue(sm.validateSession(sessionId));
		sm.invalidateSession(sessionId);
		assertFalse(sm.validateSession(sessionId));
		assertNull(sm.getUserId(sessionId));
	}

	private record GetUserIdCase(String sessionId, String expected, String description) {
		@Override public String toString() { return description; }
	}

	private static Stream<GetUserIdCase> getUserIdCases() {
		return Stream.of(
			new GetUserIdCase("valid", "user1", "Returns userId for known sessionId"),
			new GetUserIdCase(null, null, "Null sessionId returns null"),
			new GetUserIdCase("not-in-map", null, "Unknown sessionId returns null")
		);
	}

    @ParameterizedTest(name = "{index} - {0}")
	@MethodSource("getUserIdCases")
	void testGetUserId(GetUserIdCase tc) {
		SessionManagerImpl sm = new SessionManagerImpl();
		String validId = sm.createSession("user1");
		String sessionId = tc.sessionId;
		if ("valid".equals(sessionId)) sessionId = validId;
		assertEquals(tc.expected, sm.getUserId(sessionId));
	}
}
