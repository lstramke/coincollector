package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.Requests.LoginRequest;
import io.github.lstramke.coincollector.services.SessionManager;
import io.github.lstramke.coincollector.services.UserStorageService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LoginHandlerTest {

    @FunctionalInterface
    interface MockSetup {
        void setup(UserStorageService service, SessionManager sessionManager, ObjectMapper mapper) throws Exception;
    }

	private record LoginHandleTestcase(
		String method,
        String path,
		String requestBody,
		MockSetup mockSetup,
		int expectedStatus,
		String expectedResponseBody,
		String description
	) {
		@Override
		public String toString() { return description; }
	}


    private static Stream<LoginHandleTestcase> loginHandleTestcases() {
        return Stream.of(
            new LoginHandleTestcase(
                "POST",
                "/api/login",
                "{\"username\":\"test.user\"}",
                (service, sessionManager, mapper) -> {
                    var user = mock(User.class);
                    when(mapper.readValue(any(String.class), eq(LoginRequest.class)))
                        .thenReturn(new LoginRequest("test.user"));
                    when(service.getByUsername("test.user")).thenReturn(user);
                    when(user.getId()).thenReturn("user-1");
                    when(sessionManager.createSession("user-1")).thenReturn("session-abc");
                },
                200,
                null,
                "Happy path: valid login returns 200, sets cookie and content-type"
            ),
            new LoginHandleTestcase(
                "GET",
                "/api/login",
                null,
                (service, sessionManager, mapper) -> {},
                405,
                null,
                "Unsupported method: GET returns 405"
            ),
            new LoginHandleTestcase(
                "POST",
                "/api/login",
                "{\"username\":\"broken\"}",
                (service, sessionManager, mapper) -> {
                    when(mapper.readValue(any(String.class), eq(LoginRequest.class)))
                        .thenThrow(new JacksonException("fail"){});
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "JacksonException: returns 400 and error json"
            ),
            new LoginHandleTestcase(
                "POST",
                "/api/login",
                "{\"username\":\"notfound\"}",
                (service, sessionManager, mapper) -> {
                    when(mapper.readValue(any(String.class), eq(LoginRequest.class)))
                        .thenReturn(new LoginRequest("notfound"));
                    when(service.getByUsername("notfound"))
                        .thenThrow(new UserNotFoundException("not found"));
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "UserNotFoundException: returns 400 and error json"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("loginHandleTestcases")
    void TestHandle(LoginHandleTestcase testcase) throws IOException {
        var service = mock(UserStorageService.class);
        var manager = mock(SessionManager.class);
        var mapper = mock(ObjectMapper.class);
        LoginHandler handler = new LoginHandler(service, manager, mapper);
        var responseStream = new ByteArrayOutputStream();

        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getRequestMethod()).thenReturn(testcase.method());
        lenient().when(exchange.getRequestURI()).thenReturn(URI.create(testcase.path()));
        lenient().when(exchange.getResponseBody()).thenReturn(responseStream);

        if(testcase.requestBody != null){
            when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(testcase.requestBody.getBytes()));
        }

        try {
            testcase.mockSetup.setup(service, manager, mapper);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(testcase.expectedStatus), anyLong());
        if (testcase.expectedStatus == 200) {
            assertEquals("sessionId=session-abc; Path=/; HttpOnly; SameSite=Strict", headers.getFirst("Set-Cookie"));
        } else if (testcase.expectedStatus == 400) {
            assertEquals(testcase.expectedResponseBody, responseStream.toString());
        } else if (testcase.expectedStatus == 405) {
            assertEquals(0, headers.size(), "No headers should be set for 405");
        }
    }

}
