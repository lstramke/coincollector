package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserNotFoundException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.security.SecurityConfig;
import io.github.lstramke.coincollector.services.SessionManager;
import io.github.lstramke.coincollector.services.UserStorageService;

@WebMvcTest(value = LoginHandler.class)
@Import(SecurityConfig.class)
class LoginHandlerTest {

    @MockitoBean
    UserStorageService userService;

    @MockitoBean
    SessionManager sessionManager;

    @MockitoBean 
    PasswordEncoder passwordEncoder;

    @Autowired
    MockMvc mockMvc;

    @FunctionalInterface
    interface MockSetup {
        void setup(UserStorageService service, SessionManager sessionManager, PasswordEncoder passwordEncoder) throws Exception;
    }

	private record LoginHandleTestcase(
		String method,
		String requestBody,
		MockSetup mockSetup,
		int expectedStatus,
		String expectedResponseBody,
		String expectedCookie,
		String description
	) {
		@Override
		public String toString() { return description; }
	}

    private static Stream<LoginHandleTestcase> loginHandleTestcases() {
        return Stream.of(
            new LoginHandleTestcase(
                "POST",
                "{\"username\":\"testuser\",\"password\":\"test-password\"}",
                (service, sessionManager, passwordEncoder) -> {
                    var user = mock(User.class);
                    when(service.getByUsername("testuser")).thenReturn(user);
                    when(user.getId()).thenReturn("user-1");
                    when(user.getPasswordHash()).thenReturn("stored-password-hash");
                    when(passwordEncoder.matches("test-password", "stored-password-hash")).thenReturn(true);
                    when(sessionManager.createSession("user-1")).thenReturn("session-abc");
                },
                200,
                null,
                "sessionId=session-abc; Path=/; HttpOnly; SameSite=Strict",
                "Happy path: valid login returns 200, sets cookie and content-type"
            ),
            new LoginHandleTestcase(
                "GET",
                null,
                (service, sessionManager, passwordEncoder) -> {},
                405,
                null,
                null,
                "Unsupported method: GET returns 405"
            ),
            new LoginHandleTestcase(
                "POST",
                "{\"username\":",
                (service, sessionManager, passwordEncoder) -> {},
                400,
                "{\"error\":\"Request is not valid\"}",
                null,
                "Invalid JSON: returns 400 and error json"
            ),
            new LoginHandleTestcase(
                "POST",
                "{\"username\":\"testuser\",\"password\":\"short\"}",
                (service, sessionManager, passwordEncoder) -> {},
                400,
                "{\"error\":\"Request is not valid\"}",
                null,
                "To short password"
            ),
            new LoginHandleTestcase(
                "POST",
                "{\"username\":\"notfound\",\"password\":\"test-password\"}",
                (service, sessionManager, passwordEncoder) -> {
                    when(service.getByUsername("notfound"))
                        .thenThrow(new UserNotFoundException("not found"));
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                null,
                "UserNotFoundException: returns 400 and error json"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("loginHandleTestcases")
    void testLoginHandler(LoginHandleTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(userService, sessionManager, passwordEncoder);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var requestBuilder = switch (testcase.method()) {
            case "POST" -> post("/api/v1/login")
                .contentType("application/json")
                .content(testcase.requestBody == null ? "" : testcase.requestBody);
            case "GET" -> get("/api/v1/login");
            default -> throw new IllegalArgumentException("Unsupported method: " + testcase.method());
        };

        var resultActions = mockMvc.perform(requestBuilder).andExpect(status().is(testcase.expectedStatus));

        if (testcase.expectedCookie != null) {
            resultActions.andExpect(header().string("Set-Cookie", testcase.expectedCookie));
            verify(userService, times(1)).getByUsername("testuser");
            verify(sessionManager, times(1)).createSession("user-1");
        } else {
            resultActions.andExpect(header().doesNotExist("Set-Cookie"));
        }

        if (testcase.expectedResponseBody != null) {
            resultActions.andExpect(content().json(testcase.expectedResponseBody));
        }
    }

}
