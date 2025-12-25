package io.github.lstramke.coincollector.services;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.stream.Stream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SessionFilterTest {

    private record SessionFilterTestcase(
        String cookieHeader,
        boolean sessionValid,
        boolean handlerThrows,
        int expectedStatus,
        String expectedBody,
        String description
    ) {
        @Override
        public String toString() { return description; }
    }

    private static Stream<SessionFilterTestcase> sessionFilterTestcases() {
        return Stream.of(
            new SessionFilterTestcase("sessionId=abc123", true, false, 200, null, "Valid session: handler is called"),
            new SessionFilterTestcase("sessionId=abc123", false, false, 302, null, "Invalid session: redirect"),
            new SessionFilterTestcase("sessionId=abc123", true, true, 500, "{\"error\":\"An unexpected error occurred\"}", "Handler throws exception: 500 and error message"),
            new SessionFilterTestcase(null, false, false, 302, null, "No cookie: redirect"),
            new SessionFilterTestcase("foo=bar; sessionId=abc123", true, false, 200, null, "Valid session: sessionId after another cookie"),
            new SessionFilterTestcase("sessionId=abc123; foo=bar", true, false, 200, null, "Valid session: sessionId before another cookie"),
            new SessionFilterTestcase("foo=bar", false, false, 302, null, "No sessionId in cookies: redirect"),
            new SessionFilterTestcase("sessionId=; foo=bar", false, false, 302, null, "Empty sessionId: redirect")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("sessionFilterTestcases")
    void testWithSessionValidation(SessionFilterTestcase tc) throws Exception {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers requestHeaders = new Headers();
        if (tc.cookieHeader != null){
            requestHeaders.add("Cookie", tc.cookieHeader);
        }
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);

        Headers responseHeaders = new Headers();
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);

        when(exchange.getRemoteAddress()).thenReturn(new InetSocketAddress(1234));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        SessionManager sessionManager = mock(SessionManager.class);
        when(sessionManager.validateSession(any())).thenReturn(tc.sessionValid);
        when(sessionManager.getUserId(any())).thenReturn("user-1");

        HttpHandler handler = mock(HttpHandler.class);
        if (tc.handlerThrows) {
            doThrow(new RuntimeException("fail")).when(handler).handle(exchange);
        }

        HttpHandler filtered = SessionFilter.withSessionValidation(handler, sessionManager);

        filtered.handle(exchange);

        if (tc.expectedStatus == 200) {
            verify(handler).handle(exchange);
            verify(exchange, never()).sendResponseHeaders(eq(302), anyLong());
        } else {
            verify(exchange).sendResponseHeaders(eq(tc.expectedStatus), anyLong());
            if (tc.expectedBody != null) {
                assertTrue(os.toString().contains(tc.expectedBody));
            }
        }
    }
}
