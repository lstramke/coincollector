package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lstramke.coincollector.security.SecurityConfig;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.services.UserStorageService;
import jakarta.servlet.http.Cookie;
import io.github.lstramke.coincollector.services.SessionManager;

@WebMvcTest(value = PasswordHandler.class)
@Import(SecurityConfig.class)
public class PasswordHandlerTest {

    @MockitoBean 
    UserStorageService userStorageService;

    @MockitoBean 
    PasswordEncoder passwordEncoder;

    @MockitoBean
    SessionManager sessionManager;

    @Autowired
    MockMvc mockMvc;

    @FunctionalInterface
    interface MockSetup {
        void setup(UserStorageService service, PasswordEncoder passwordEncoder) throws Exception;
    }

    private record PasswordSetupTestcase(
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

    private static Stream<PasswordSetupTestcase> setupPasswordTestcases() {
        return Stream.of(
            new PasswordSetupTestcase(
                "POST",
                "{\"username\":\"testuser\",\"newPassword\":\"test-password\"}",
                (service, passwordEncoder) -> {
                    var user = new User("testuser");
                    when(service.getByUsername("testuser")).thenReturn(user);
                    when(passwordEncoder.encode("test-password")).thenReturn("encoded-password");
                },
                204,
                null,
                null,
                "Happy path: valid password setup returns 204 without a cookie or response body"
            ),
            new PasswordSetupTestcase(
                "GET",
                null,
                (service, passwordEncoder) -> {},
                405,
                "{\"error\":\"Method is not allowed\"}",
                null,
                "Unsupported method: GET returns 405 and error json"
            ),
            new PasswordSetupTestcase(
                "POST",
                "{\"username\":",
                (service, passwordEncoder) -> {},
                400,
                "{\"error\":\"Request is not valid\"}",
                null,
                "Invalid JSON: returns 400 and error json"
            ),
            new PasswordSetupTestcase(
                "POST",
                "{\"username\":\"testuser\",\"newPassword\":\"short\"}",
                (service, passwordEncoder) -> {},
                400,
                "{\"error\":\"Request is not valid\"}",
                null,
                "Password shorter than eight characters: returns 400 and error json"
            ),
            new PasswordSetupTestcase(
                "POST",
                "{\"username\":\"testuser\",\"newPassword\":\"test-password\"}",
                (service, passwordEncoder) -> {
                    var user = new User("testuser", "existing-password-hash");
                    when(service.getByUsername("testuser")).thenReturn(user);
                },
                409,
                "{\"error\":\"Password is already set\"}",
                null,
                "Existing password: returns 409 and error json"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("setupPasswordTestcases")
    void testPasswordSetup(PasswordSetupTestcase testcase) throws Exception { 
        try {
            testcase.mockSetup.setup(userStorageService, passwordEncoder);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var requestBuilder = switch (testcase.method()) {
            case "POST" -> post("/api/v1/password/setup")
                .contentType("application/json")
                .content(testcase.requestBody == null ? "" : testcase.requestBody);
            case "GET" -> get("/api/v1/password/setup");
            default -> throw new IllegalArgumentException("Unsupported method: " + testcase.method());
        };

        var resultActions = mockMvc.perform(requestBuilder).andExpect(status().is(testcase.expectedStatus));

        resultActions.andExpect(header().doesNotExist("Set-Cookie"));

        if (testcase.expectedResponseBody != null) {
            resultActions.andExpect(content().json(testcase.expectedResponseBody));
        }

        if (testcase.expectedStatus == 204) {
            verify(userStorageService).getByUsername("testuser");
            verify(passwordEncoder).encode("test-password");
            verify(userStorageService).update(any(User.class));
        }
    }

    @FunctionalInterface
    interface MockSetupChangePassword {
        void setup(
            UserStorageService service,
            PasswordEncoder passwordEncoder,
            SessionManager sessionManager
        ) throws Exception;
    }
    
    private record PasswordChangeTestcase(
        String requestBody,
        String sessionId,
        MockSetupChangePassword mockSetup,
        int expectedStatus,
        String expectedResponseBody,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }
    
    private static Stream<PasswordChangeTestcase> changePasswordTestcases() {
        return Stream.of(
            new PasswordChangeTestcase(
                "{\"oldPassword\":\"old-password\",\"newPassword\":\"new-password\"}",
                "valid-session",
                (service, passwordEncoder, sessionManager) -> {
                    when(sessionManager.validateSession("valid-session"))
                        .thenReturn(true);
                    when(sessionManager.getUserId("valid-session"))
                        .thenReturn("testuser");
                
                    var user = new User("testuser", "old-password-hash");
                
                    when(service.getById("testuser")).thenReturn(user);
                    when(passwordEncoder.matches(
                        "old-password",
                        "old-password-hash"
                    )).thenReturn(true);
                    when(passwordEncoder.encode("new-password"))
                        .thenReturn("new-password-hash");
                },
                204,
                null,
                "Happy path: valid password change returns 204"
            ),
        
            new PasswordChangeTestcase(
                "{\"oldPassword\":\"wrong-password\",\"newPassword\":\"new-password\"}",
                "valid-session",
                (service, passwordEncoder, sessionManager) -> {
                    when(sessionManager.validateSession("valid-session"))
                        .thenReturn(true);
                    when(sessionManager.getUserId("valid-session"))
                        .thenReturn("testuser");
                
                    var user = new User("testuser", "old-password-hash");
                
                    when(service.getById("testuser")).thenReturn(user);
                    when(passwordEncoder.matches(
                        "wrong-password",
                        "old-password-hash"
                    )).thenReturn(false);
                },
                403,
                "{\"error\":\"Wrong password.\"}",
                "Wrong old password: returns 403"
            ),
        
            new PasswordChangeTestcase(
                "{\"oldPassword\":\"old-password\",\"newPassword\":\"short\"}",
                "valid-session",
                (service, passwordEncoder, sessionManager) -> {
                    when(sessionManager.validateSession("valid-session"))
                        .thenReturn(true);
                    when(sessionManager.getUserId("valid-session"))
                        .thenReturn("testuser");
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "New password shorter than eight characters: returns 400"
            ),
        
            new PasswordChangeTestcase(
                "{\"oldPassword\":",
                "valid-session",
                (service, passwordEncoder, sessionManager) -> {
                    when(sessionManager.validateSession("valid-session"))
                        .thenReturn(true);
                    when(sessionManager.getUserId("valid-session"))
                        .thenReturn("testuser");
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "Invalid JSON: returns 400"
            ),
        
            new PasswordChangeTestcase(
                "{\"oldPassword\":\"old-password\",\"newPassword\":\"new-password\"}",
                null,
                (service, passwordEncoder, sessionManager) -> {},
                401,
                "{\"error\":\"Unauthorized\"}",
                "No session cookie: returns 401"
            ),
        
            new PasswordChangeTestcase(
                "{\"oldPassword\":\"old-password\",\"newPassword\":\"new-password\"}",
                "invalid-session",
                (service, passwordEncoder, sessionManager) -> {
                    when(sessionManager.validateSession("invalid-session"))
                        .thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "Invalid session: returns 401"
            )
        );
    }
    
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("changePasswordTestcases")
    void testPasswordChange(PasswordChangeTestcase testcase) throws Exception {
    
        try {
            testcase.mockSetup.setup(userStorageService,passwordEncoder,sessionManager);
        } catch (Exception e) {
            fail("due to unexpected exception in setup");
        }
    
        var requestBuilder = post("/api/v1/password/change")
            .contentType("application/json")
            .content(testcase.requestBody());
    
        if (testcase.sessionId() != null) {
            requestBuilder.cookie(
                new Cookie("sessionId", testcase.sessionId())
            );
        }
    
        var resultActions = mockMvc.perform(requestBuilder)
            .andExpect(status().is(testcase.expectedStatus()));
    
        if (testcase.expectedResponseBody() != null) {
            resultActions.andExpect(
                content().json(testcase.expectedResponseBody())
            );
        }
    
        if (testcase.expectedStatus() == 204) {
            verify(userStorageService).getById("testuser");
            verify(passwordEncoder).matches(
                "old-password",
                "old-password-hash"
            );
            verify(passwordEncoder).encode("new-password");
            verify(userStorageService).update(any(User.class));
        }
    }
}
