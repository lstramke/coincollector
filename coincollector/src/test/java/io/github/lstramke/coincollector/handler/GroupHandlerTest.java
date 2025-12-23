package io.github.lstramke.coincollector.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.net.httpserver.HttpExchange;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;

@ExtendWith(MockitoExtension.class)
class GroupHandlerTest {

    @FunctionalInterface
    interface MockSetup {
        void setup(EuroCoinCollectionGroupStorageService service) throws Exception;
    }

    private static final String PREFIX = "/api/groups";
    private static final String USER_ID = "user-1";
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";

    private record GroupHandleTestcase(
        String method,
        String path,
        String requestBody,
        String userId,
        MockSetup mockSetup,
        int expectedStatus,
        String expectedResponseBody,
        String description
    ) {
        @Override
        public String toString() { 
            return description; 
        }
    }

    private static Stream<GroupHandleTestcase> groupHandleTestcases() {
        return Stream.of(
            new GroupHandleTestcase(
                "PUT",
                PREFIX,
                null,
                USER_ID,
                service -> {},
                405,
                null,
                "Default-Fall: Nicht unterstützte Methode → 405"
            ),
            new GroupHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                service -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById("123e4567-e89b-12d3-a456-426614174000")).thenReturn(groupMock);
                    doNothing().when(service).delete("123e4567-e89b-12d3-a456-426614174000");
                },
                204,
                "",
                "Success case: DELETE group returns 204"
            ),
            new GroupHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                service -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE group fails owner check and returns 404"
            ),
            new GroupHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                service -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doThrow(new EuroCoinCollectionGroupDeleteException("fail")).when(service).delete(VALID_UUID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE group triggers delete exception and returns 500"
            ),
            new GroupHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                service -> {
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE group triggers not found exception and returns 404"
            )
            
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("groupHandleTestcases")
    void testHandle(GroupHandleTestcase testcase) throws IOException {
        EuroCoinCollectionGroupStorageService service = mock(EuroCoinCollectionGroupStorageService.class); 
        GroupHandler handler = new GroupHandler(service);
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        HttpExchange exchange = mock(HttpExchange.class);

        lenient().when(exchange.getRequestMethod()).thenReturn(testcase.method());
        lenient().when(exchange.getRequestURI()).thenReturn(java.net.URI.create(testcase.path()));
        lenient().when(exchange.getAttribute("userId")).thenReturn(testcase.userId());
        lenient().when(exchange.getResponseBody()).thenReturn(responseStream);

        if(testcase.requestBody != null){
            when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(testcase.requestBody.getBytes()));
        }

        try {
            testcase.mockSetup.setup(service);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setuo");
        }

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(testcase.expectedStatus), anyLong());

        if (testcase.expectedResponseBody != null) {
            String actualBody = responseStream.toString(StandardCharsets.UTF_8);
            org.junit.jupiter.api.Assertions.assertEquals(
                testcase.expectedResponseBody.trim(),
                actualBody.trim(),
                "Response body does not match!"
            );
        }
    }
}
