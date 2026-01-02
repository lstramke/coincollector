package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import io.github.lstramke.coincollector.services.SessionManager;

public class LogoutHandlerTest {
    @FunctionalInterface
    interface MockSetup {
        void setup(SessionManager sessionManager) throws Exception;
    }

    private record LogoutHandleTestcase(
        String method,
        String path,
        String cookieHeader,
        MockSetup mockSetup,
        int expectedStatus,
        String expectedSetCookie,
        String description
    ) {
        @Override
        public String toString() { return description; }
    }

    private static Stream<LogoutHandleTestcase> logoutHandleTestcases() {
        return Stream.of(
            new LogoutHandleTestcase(
                "POST",
                "/api/logout",
                "sessionId=session-abc",
                sessionManager -> {
                    doNothing().when(sessionManager).invalidateSession("session-abc");
                },
                204,
                "sessionId=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict",
                "Happy path: valid session, logout returns 204 and deletes cookie"
            ),
            new LogoutHandleTestcase(
                "POST",
                "/api/logout",
                null,
                sessionManager -> {},
                401,
                null,
                "No session cookie: returns 401"
            ),
            new LogoutHandleTestcase(
                "POST",
                "/api/logout",
                "sessionId=",
                sessionManager -> {
                    doNothing().when(sessionManager).invalidateSession(null);
                },
                401,
                null,
                "Empty session: returns 401"
            ),
            new LogoutHandleTestcase(
                "GET",
                "/api/logout",
                "sessionId=session-abc",
                sessionManager -> {},
                405,
                null,
                "Unsupported method: GET returns 405"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("logoutHandleTestcases")
    void testHandle(LogoutHandleTestcase testcase) throws IOException {
        var sessionManager = mock(SessionManager.class);
        LogoutHandler handler = new LogoutHandler(sessionManager);
        var responseStream = new ByteArrayOutputStream();

        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(headers);
        when(exchange.getRequestMethod()).thenReturn(testcase.method());
        when(exchange.getRequestURI()).thenReturn(URI.create(testcase.path()));
        when(exchange.getResponseBody()).thenReturn(responseStream);

        if (testcase.cookieHeader() != null) {
            Headers reqHeaders = new Headers();
            reqHeaders.add("Cookie", testcase.cookieHeader());
            when(exchange.getRequestHeaders()).thenReturn(reqHeaders);
        } else {
            when(exchange.getRequestHeaders()).thenReturn(new Headers());
        }

        try {
            testcase.mockSetup().setup(sessionManager);
        } catch (Exception e) {
            fail("Unexpected exception in setup");
        }

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(testcase.expectedStatus()), anyLong());
        if (testcase.expectedSetCookie() != null) {
            assertEquals(testcase.expectedSetCookie(), headers.getFirst("Set-Cookie"));
        } else {
            assertNull(headers.getFirst("Set-Cookie"));
        }
    }
}
