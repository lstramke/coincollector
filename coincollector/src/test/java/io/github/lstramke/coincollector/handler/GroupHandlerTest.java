package io.github.lstramke.coincollector.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
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
import com.sun.net.httpserver.Headers;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupUpdateException;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.DTOs.Requests.CreateGroupRequest;
import io.github.lstramke.coincollector.model.DTOs.Requests.UpdateGroupRequest;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GroupHandlerTest {

    @FunctionalInterface
    interface MockSetup {
        void setup(EuroCoinCollectionGroupStorageService service, ObjectMapper mapper) throws Exception;
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

    private static Stream<GroupHandleTestcase> deleteTestcases() {
        return Stream.of(
            new GroupHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (service, mapper) -> {
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
                (service, mapper) -> {
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
                (service, mapper) -> {
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
                (service, mapper) -> {
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE group triggers not found exception and returns 404"
            )
        );
    }

    private static Stream<GroupHandleTestcase> getTestcases() {
        return Stream.of(
            new GroupHandleTestcase(
                "GET",
                PREFIX + "/invalid-path",
                null,
                USER_ID,
                (service, mapper) -> {},
                405,
                null,
                "GET with invalid path returns 405 (neither getAll nor getById)"
            ),
            new GroupHandleTestcase(
                "GET",
                PREFIX,
                null,
                USER_ID,
                (service, mapper) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn("id-1");
                    when(groupMock.getName()).thenReturn("test group");
                    when(groupMock.getCollections()).thenReturn(java.util.List.of());
                    when(service.getAllByUser(USER_ID)).thenReturn(java.util.List.of(groupMock));
                    when(mapper.writeValueAsString(any())).thenReturn("[{\"id\":\"id-1\",\"name\":\"test group\",\"collections\":[]}]");
                },
                200,
                "[{\"id\":\"id-1\",\"name\":\"test group\",\"collections\":[]}]",
                "GET all groups returns 200 and JSON array"
            ),
            new GroupHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (service, mapper) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn(VALID_UUID);
                    when(groupMock.getName()).thenReturn("test group");
                    when(groupMock.getCollections()).thenReturn(java.util.List.of());
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    when(mapper.writeValueAsString(any())).thenReturn("{\"id\":\"" + VALID_UUID + "\",\"name\":\"test group\",\"collections\":[]}");
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"name\":\"test group\",\"collections\":[]}",
                "GET by ID returns 200 and JSON object"
            ),
            new GroupHandleTestcase(
                "GET",
                PREFIX,
                null,
                USER_ID,
                (service, mapper) -> when(service.getAllByUser(USER_ID)).thenThrow(new EuroCoinCollectionGroupGetAllException()),
                500,
                "{\"error\":\"Internal server error\"}",
                "GET all groups triggers GetAllException and returns 500"
            ),
            new GroupHandleTestcase(
                "GET",
                PREFIX,
                null,
                USER_ID,
                (service, mapper) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn("id-1");
                    when(groupMock.getName()).thenReturn("test group");
                    when(groupMock.getCollections()).thenReturn(java.util.List.of());
                    when(service.getAllByUser(USER_ID)).thenReturn(java.util.List.of(groupMock));
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail") {});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET all groups triggers JacksonException and returns 500"
            ),
            new GroupHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (service, mapper) -> when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found")),
                404,
                "{\"error\":\"Resource not found\"}",
                "GET by ID triggers NotFoundException and returns 404"
            ),
            new GroupHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (service, mapper) -> when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupGetByIdException(VALID_UUID)),
                500,
                "{\"error\":\"Internal server error\"}",
                "GET by ID triggers GetByIdException and returns 500"
            ),
            new GroupHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (service, mapper) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET by ID fails owner check and returns 404"
            )
        );
    }

    private static Stream<GroupHandleTestcase> postTestcases() {
        return Stream.of(
            new GroupHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"test group\"}",
                USER_ID,
                (service, mapper) -> {
                    var request = mock(CreateGroupRequest.class);
                    when(request.name()).thenReturn("test group");
                    when(mapper.readValue(any(String.class), eq(CreateGroupRequest.class))).thenReturn(request);
                    doNothing().when(service).save(any(EuroCoinCollectionGroup.class));
                    when(mapper.writeValueAsString(any())).thenReturn("{\"id\":\"some-id\",\"name\":\"test group\",\"collections\":[]}");
                },
                201,
                "{\"id\":\"some-id\",\"name\":\"test group\",\"collections\":[]}",
                "POST create group: Happy Path"
            ),
            new GroupHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"test group\"}",
                USER_ID,
                (service, mapper) -> {
                    when(mapper.readValue(any(String.class), eq(CreateGroupRequest.class))).thenThrow(new JacksonException("fail") {});
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "POST create group: JacksonException during deserialization (400)"
            ),
            new GroupHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"test group\"}",
                USER_ID,
                (service, mapper) -> {
                    var request = mock(CreateGroupRequest.class);
                    when(request.name()).thenReturn("test group");
                    when(mapper.readValue(any(String.class), eq(CreateGroupRequest.class))).thenReturn(request);
                    doNothing().when(service).save(any(EuroCoinCollectionGroup.class));
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail") {});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST create group: JacksonException during serialization (500)"
            ),
            new GroupHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"test group\"}",
                USER_ID,
                (service, mapper) -> {
                    var request = mock(CreateGroupRequest.class);
                    when(request.name()).thenReturn("test group");
                    when(mapper.readValue(any(String.class), eq(CreateGroupRequest.class))).thenReturn(request);
                    doThrow(new EuroCoinCollectionGroupSaveException("fail")).when(service).save(any(EuroCoinCollectionGroup.class));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST create group: EuroCoinCollectionGroupSaveException (500)"
            )
        );
    }

    private static Stream<GroupHandleTestcase> patchTestcases() {
        return Stream.of(
            new GroupHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, mapper) -> {
                    var updateRequest = mock(UpdateGroupRequest.class);
                    when(updateRequest.name()).thenReturn("new group name");
                    when(mapper.readValue(any(String.class), eq(UpdateGroupRequest.class))).thenReturn(updateRequest);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(groupMock.getName()).thenReturn("new group name");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doNothing().when(service).updateMetadata(groupMock);
                    when(mapper.writeValueAsString(any())).thenReturn("{\"name\":\"new group name\"}");
                },
                200,
                "{\"name\":\"new group name\"}",
                "PATCH update group: Happy Path"
            ),
            new GroupHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, mapper) -> {
                    when(mapper.readValue(any(String.class), eq(UpdateGroupRequest.class))).thenThrow(new JacksonException("fail") {});
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "PATCH update group: JacksonException during deserialization (400)"
            ),
            new GroupHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, mapper) -> {
                    var updateRequest = mock(UpdateGroupRequest.class);
                    when(updateRequest.name()).thenReturn("new group name");
                    when(mapper.readValue(any(String.class), eq(UpdateGroupRequest.class))).thenReturn(updateRequest);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(groupMock.getName()).thenReturn("new group name");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doNothing().when(service).updateMetadata(groupMock);
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail") {});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH update group: JacksonException during serialization (500)"
            ),
            new GroupHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, mapper) -> {
                    var updateRequest = mock(UpdateGroupRequest.class);
                    when(mapper.readValue(any(String.class), eq(UpdateGroupRequest.class))).thenReturn(updateRequest);
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH update group: NotFoundException (404)"
            ),
            new GroupHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, mapper) -> {
                    var updateRequest = mock(UpdateGroupRequest.class);
                    when(mapper.readValue(any(String.class), eq(UpdateGroupRequest.class))).thenReturn(updateRequest);
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupGetByIdException(VALID_UUID));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH update group: GetByIdException (500)"
            ),
            new GroupHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, mapper) -> {
                    var updateRequest = mock(UpdateGroupRequest.class);
                    when(mapper.readValue(any(String.class), eq(UpdateGroupRequest.class))).thenReturn(updateRequest);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doThrow(new EuroCoinCollectionGroupUpdateException("fail")).when(service).updateMetadata(groupMock);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH update group: UpdateException (500)"
            ),
            new GroupHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, mapper) -> {
                    var updateRequest = mock(UpdateGroupRequest.class);
                    when(mapper.readValue(any(String.class), eq(UpdateGroupRequest.class))).thenReturn(updateRequest);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH update group: Owner check fails (404)"
            )
        );
    }

    private static Stream<GroupHandleTestcase> groupHandleTestcases() {
        return Stream.concat(
            Stream.of(
                new GroupHandleTestcase(
                    "PUT",
                    PREFIX,
                    null,
                    USER_ID,
                    (service, mapper) -> {},
                    405,
                    null,
                    "Default case: unsupported method returns 405"
                )
            ),
            Stream.of(
                deleteTestcases(),
                getTestcases(),
                postTestcases(),
                patchTestcases()
            ).flatMap(s -> s)
        );
    }
    
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("groupHandleTestcases")
    void testHandle(GroupHandleTestcase testcase) throws IOException {
        var service = mock(EuroCoinCollectionGroupStorageService.class);
        var mapper = mock(ObjectMapper.class);
        GroupHandler handler = new GroupHandler(service, mapper);
        var responseStream = new ByteArrayOutputStream();

        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = new Headers();
        lenient().when(exchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(exchange.getRequestMethod()).thenReturn(testcase.method());
        lenient().when(exchange.getRequestURI()).thenReturn(URI.create(testcase.path()));
        lenient().when(exchange.getAttribute("userId")).thenReturn(testcase.userId());
        lenient().when(exchange.getResponseBody()).thenReturn(responseStream);

        if(testcase.requestBody != null){
            when(exchange.getRequestBody()).thenReturn(new ByteArrayInputStream(testcase.requestBody.getBytes()));
        }

        try {
            testcase.mockSetup.setup(service, mapper);
        } catch (Exception e) {
            fail("fail due to unexcpected exception in setup");
        }

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(testcase.expectedStatus), anyLong());

        if (testcase.expectedResponseBody != null) {
            String actualBody = responseStream.toString(StandardCharsets.UTF_8);
            assertEquals(
                testcase.expectedResponseBody.trim(),
                actualBody.trim(),
                "Response body does not match!"
            );
        }
    }
}
