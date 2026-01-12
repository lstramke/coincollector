package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import io.github.lstramke.coincollector.exceptions.userExceptions.UserSaveException;
import io.github.lstramke.coincollector.model.User;
import io.github.lstramke.coincollector.model.DTOs.Requests.RegistrationRequest;
import io.github.lstramke.coincollector.services.SessionManager;
import io.github.lstramke.coincollector.services.UserStorageService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RegistrationHandlerTest {

	@FunctionalInterface
	interface MockSetup {
		void setup(UserStorageService service, SessionManager sessionManager, ObjectMapper mapper) throws Exception;
	}

	   private record RegistrationHandleTestcase(
		   String method,
		   String path,
		   String requestBody,
		   MockSetup mockSetup,
		   int expectedStatus,
		   String expectedResponseBody,
		   Map<String, String> expectedHeaders,
		   String description
	   ) {
		   @Override
		   public String toString() { return description; }
	   }

	   private static Stream<RegistrationHandleTestcase> registrationHandleTestcases() {
		   return Stream.of(
			    new RegistrationHandleTestcase(
				    "POST",
				    "/api/registration",
				    "{\"username\":\"test.user\"}",
					(service, sessionManager, mapper) -> {
					    var registrationRequest = new RegistrationRequest("test.user");
					    when(mapper.readValue(any(String.class), eq(RegistrationRequest.class))).thenReturn(registrationRequest);
					    when(sessionManager.createSession(anyString())).thenReturn("session-abc");
					},
				    201,
				    null,
				    Map.of(
					   "Set-Cookie", "sessionId=session-abc; Path=/; HttpOnly; SameSite=Strict"
				    ),
				    "Happy path: valid registration returns 201, sets cookie and content-type"
			    ),
			    new RegistrationHandleTestcase(
				    "GET",
				    "/api/registration",
				    null,
				    (service, sessionManager, mapper) -> {},
				    405,
				    null,
				    Map.of(),
				    "Unsupported method: GET returns 405"
			    ),
			    new RegistrationHandleTestcase(
				    "POST",
				    "/api/registration",
				    "{\"username\":\"broken\"}",
				    (service, sessionManager, mapper) -> {
				       when(mapper.readValue(any(String.class), eq(RegistrationRequest.class))).thenThrow(new JacksonException("fail"){});
				    },
				    400,
				    "{\"error\":\"Request is not valid\"}",
				    Map.of("Content-Type", "application/json"),
				    "JacksonException: returns 400 and error json"
			    ),
			    new RegistrationHandleTestcase(
				    "POST",
				    "/api/registration",
				    "{\"username\":\"fail\"}",
				      (service, sessionManager, mapper) -> {
				    	var registrationRequest = new RegistrationRequest("fail");
				    	when(mapper.readValue(any(String.class), eq(RegistrationRequest.class))).thenReturn(registrationRequest);
				    	doThrow(new UserSaveException("fail")).when(service).save(any(User.class));
				    },
				    500,
				    "{\"error\":\"An unexpected error occurred\"}",
				    Map.of("Content-Type", "application/json"),
				    "UserSaveException: returns 500 and error json"
			)
		);   
	}    

	@ParameterizedTest(name = "{index} - {0}")
	@MethodSource("registrationHandleTestcases")
	void TestHandle(RegistrationHandleTestcase testcase) throws IOException {
		var service = mock(UserStorageService.class);
		var manager = mock(SessionManager.class);
		var mapper = mock(ObjectMapper.class);
		RegistrationHandler handler = new RegistrationHandler(service, manager, mapper);
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
		if (testcase.expectedStatus == 201) {
		    verify(service).save(any(User.class));
		}
		if (testcase.expectedHeaders != null) {
		    for (var entry : testcase.expectedHeaders.entrySet()) {
			    assertEquals(entry.getValue(), headers.getFirst(entry.getKey()), "Header " + entry.getKey());
		    }
		    assertEquals(testcase.expectedHeaders.size(), headers.size(), "Header count");
		}
		if (testcase.expectedStatus == 400 || testcase.expectedStatus == 500) {
		    assertEquals(testcase.expectedResponseBody, responseStream.toString());
		} else if (testcase.expectedStatus == 405) {
		    assertEquals(0, headers.size(), "No headers should be set for 405");
		}
	}

}
