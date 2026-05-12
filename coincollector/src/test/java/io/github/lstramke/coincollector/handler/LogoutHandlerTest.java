package io.github.lstramke.coincollector.handler;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import io.github.lstramke.coincollector.security.SecurityConfig;
import io.github.lstramke.coincollector.services.SessionManager;

@WebMvcTest(value = LogoutHandler.class)
@Import(SecurityConfig.class)
public class LogoutHandlerTest {

    @MockitoBean
    SessionManager sessionManager;

    @Autowired
    MockMvc mockMvc;

    @FunctionalInterface
    interface MockSetup {
        void setup(SessionManager sessionManager) throws Exception;
    }

    private record LogoutHandlerTestcase(
        String method,
        String endpoint,
        String sessionId,
        MockSetup mockSetup,
        int expectedStatus,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<LogoutHandlerTestcase> logoutHandlerTestcases() {
        return Stream.of(
            new LogoutHandlerTestcase(
                "POST",
                "/api/v1/logout",
                "session-abc",
                sessionManager -> {
                    when(sessionManager.validateSession("session-abc")).thenReturn(true);
                    when(sessionManager.getUserId("session-abc")).thenReturn("testuser");
                    when(sessionManager.getSessionId("testuser")).thenReturn("session-abc");
                    doNothing().when(sessionManager).invalidateSession("session-abc");
                },
                204,
                "Happy path: authenticated user logout returns 204"
            ),
            new LogoutHandlerTestcase(
                "POST",
                "/api/v1/logout",
                null,
                sessionManager -> {},
                401,
                "No authentication: returns 401"
            ),
            new LogoutHandlerTestcase(
                "POST",
                "/api/v1/logout",
                "session-abc",
                sessionManager -> {
                    when(sessionManager.getSessionId("testuser")).thenReturn("session-abc");
                    doThrow(new RuntimeException("Session error")).when(sessionManager).invalidateSession("session-abc");
                    when(sessionManager.validateSession("session-abc")).thenReturn(true);
                    when(sessionManager.getUserId("session-abc")).thenReturn("testuser");
                },
                500,
                "Session invalidation fails: returns 500"
            ),
            new LogoutHandlerTestcase(
                "GET",
                "/api/v1/logout",
                "session-abc",
                sessionManager -> {
                    when(sessionManager.validateSession("session-abc")).thenReturn(true);
                    when(sessionManager.getUserId("session-abc")).thenReturn("testuser");
                },
                405,
                "Unsupported method: GET returns 405"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("logoutHandlerTestcases")
    void testLogoutHandler(LogoutHandlerTestcase testcase) throws Exception {
        try {
            testcase.mockSetup().setup(sessionManager);
        } catch (Exception e) {
            fail("Unexpected exception in setup");
        }

        var requestBuilder = switch (testcase.method()) {
            case "POST" -> post(testcase.endpoint());
            case "GET" -> get(testcase.endpoint());
            default -> throw new IllegalArgumentException("Unsupported method: " + testcase.method());
        };

        if (testcase.sessionId() != null) {
            requestBuilder.cookie(new Cookie("sessionId", testcase.sessionId()));
        }

        var result = mockMvc.perform(requestBuilder)
            .andExpect(status().is(testcase.expectedStatus()));

        if (testcase.expectedStatus() == 204) {
            result.andExpect(header().string("Set-Cookie", containsString("sessionId=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")));
            verify(sessionManager, times(1)).invalidateSession("session-abc");
        }
    }
}
