package io.github.lstramke.coincollector.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.services.SessionManager;
import io.github.lstramke.coincollector.services.UserStorageService;
import io.github.lstramke.security.SecurityConfig;

@WebMvcTest(value = RegistrationHandler.class)
@Import(SecurityConfig.class)
class RegistrationHandlerTest {

	@MockitoBean
    UserStorageService userService;

    @MockitoBean
    SessionManager sessionManager;

    @Autowired
    MockMvc mockMvc;

	@FunctionalInterface
	interface MockSetup {
		void setup(UserStorageService service, SessionManager sessionManager) throws Exception;
	}

	   private record RegistrationHandleTestcase(
		   String method,
		   String requestBody,
		   MockSetup mockSetup,
		   int expectedStatus,
		   String expectedResponseBody,
		   String expectedCookie,
		   boolean expectSave,
		   String description
	   ) {
		   @Override
		   public String toString() { return description; }
	   }

	   private static Stream<RegistrationHandleTestcase> registrationHandleTestcases() {
		   return Stream.of(
			    new RegistrationHandleTestcase(
				    "POST",
				    "{\"username\":\"test.user\"}",
					(service, sessionManager) -> {
					    when(sessionManager.createSession(any(String.class))).thenReturn("session-abc");
					},
				    201,
				    null,
				    "sessionId=session-abc; Path=/; HttpOnly; SameSite=Strict",
				    true,
				    "Happy path: valid registration returns 201, sets cookie and content-type"
			    ),
			    new RegistrationHandleTestcase(
				    "GET",
				    null,
				    (service, sessionManager) -> {},
				    405,
				    "{\"error\":\"Method is not allowed\"}",
				    null,
				    false,
				    "Unsupported method: GET returns 405"
			    ),
			    new RegistrationHandleTestcase(
				    "POST",
				    "{\"username\":",
				    (service, sessionManager) -> {},
				    400,
				    "{\"error\":\"Request is not valid\"}",
				    null,
				    false,
                	"Invalid JSON: returns 400 and error json"
			    ),
			    new RegistrationHandleTestcase(
				    "POST",
				    "{\"username\":\"fail\"}",
				      (service, sessionManager) -> {
				    	doThrow(new UserSaveException("fail")).when(service).save(any(User.class));
				    },
				    500,
				    "{\"error\":\"An unexpected error occurred\"}",
				    null,
				    true,
				    "UserSaveException: returns 500 and error json"
			)
		);   
	}    

	@ParameterizedTest(name = "{index} - {0}")
	@MethodSource("registrationHandleTestcases")
	void testRegistrationHandler(RegistrationHandleTestcase testcase) throws Exception {
		testcase.mockSetup.setup(userService, sessionManager);

		var requestBuilder = switch (testcase.method()) {
			case "POST" -> post("/api/v1/registration")
				.contentType("application/json")
				.content(testcase.requestBody == null ? "" : testcase.requestBody);
			case "GET" -> get("/api/v1/registration");
			default -> throw new IllegalArgumentException("Unsupported method: " + testcase.method());
		};

		var resultActions = mockMvc.perform(requestBuilder).andExpect(status().is(testcase.expectedStatus));

		if (testcase.expectedCookie != null) {
			resultActions.andExpect(header().string("Set-Cookie", testcase.expectedCookie));
		}

		if (testcase.expectedResponseBody != null) {
			resultActions.andExpect(content().json(testcase.expectedResponseBody));
		}

		if (testcase.expectSave) {
			verify(userService).save(any(User.class));
		}
	}

}
