package io.github.lstramke.coincollector.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionCoinsLoadException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.DTOs.Requests.CreateCollectionRequest;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import io.github.lstramke.coincollector.services.EuroCoinCollectionStorageService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CollectionHandlerTest {

    @FunctionalInterface
    interface MockSetup {
        void setup(EuroCoinCollectionStorageService collectionService, EuroCoinCollectionGroupStorageService groupService, ObjectMapper mapper) throws Exception;
    }

    private static final String PREFIX = "/api/collections";
    private static final String USER_ID = "user-1";
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";

    private record CollectionHandleTestcase(
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

    private static Stream<CollectionHandleTestcase> collectionHandleTestcases() {
        return Stream.of(
            new CollectionHandleTestcase(
                "PUT",
                PREFIX,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {},
                405,
                null,
                "Default case: unsupported method returns 405"
            ),
            new CollectionHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionMock.getName()).thenReturn("German Euro Coins");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);
                    when(collectionMock.getCoins()).thenReturn(List.of());
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    String responseJson = "{" +
                        "\"id\":\"" + VALID_UUID + "\"," +
                        "\"name\":\"German Euro Coins\"," +
                        "\"groupId\":\"group-1\"," +
                        "\"coins\":[]}";
                    when(mapper.writeValueAsString(any())).thenReturn(responseJson);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                "GET: Happy path, returns collection according to API spec"
            ),
            new CollectionHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other user");
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: fails owner check and returns 404"
            ),
            new CollectionHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: EuroCoinCollectionNotFoundException -> 404"
            ),
            new CollectionHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: EuroCoinCollectionGroupNotFoundException -> 404"
            ),
            new CollectionHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionMock.getName()).thenReturn("German Euro Coins");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);
                    when(collectionMock.getCoins()).thenReturn(List.of());
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail"){});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: JacksonException -> 500"
            ),
            new CollectionHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGetByIdException -> 500"
            ),
            new CollectionHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGroupGetByIdException -> 500"
            ),
            new CollectionHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.name()).thenReturn("German Euro Coins");
                    when(request.groupId()).thenReturn("group-1");
                    when(request.coins()).thenReturn(List.of());
                    when(mapper.readValue("{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}", CreateCollectionRequest.class)).thenReturn(request);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    String responseJson = "{\"id\":\"abc\",\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}";
                    when(mapper.writeValueAsString(any())).thenReturn(responseJson);
                },
                201,
                "{\"id\":\"abc\",\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                "POST: Happy path, creates collection"
            ),
            new CollectionHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.groupId()).thenReturn("group-1");
                    when(mapper.readValue("{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}", CreateCollectionRequest.class)).thenReturn(request);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other user");

                },
                404,
                "{\"error\":\"Resource not found\"}",
                "POST: fails owner check and returns 404\""
            ),
            new CollectionHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    when(mapper.readValue("{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}", CreateCollectionRequest.class)).thenThrow(new JacksonException("fail"){});
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "POST: JacksonException during deserialization -> 400"
            ),
            new CollectionHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.name()).thenReturn("German Euro Coins");
                    when(request.groupId()).thenReturn("group-1");
                    when(request.coins()).thenReturn(List.of());
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doThrow(new EuroCoinCollectionSaveException("fail")).when(collectionService).save(any(EuroCoinCollection.class));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST: EuroCoinCollectionSaveException -> 500"
            ),
            new CollectionHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.groupId()).thenReturn("group-1");
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST: EuroCoinCollectionGroupGetByIdException -> 500"
            ),
            new CollectionHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.groupId()).thenReturn("group-1");
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Parent resource not found\"}",
                "POST: EuroCoinCollectionGroupNotFoundException -> 404"
            ),
            new CollectionHandleTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"German Euro Coins\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.name()).thenReturn("German Euro Coins");
                    when(request.groupId()).thenReturn("group-1");
                    when(request.coins()).thenReturn(List.of());
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doNothing().when(collectionService).save(any(EuroCoinCollection.class));
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail"){});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST: JacksonException during serialization -> 500"
            ),
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.name()).thenReturn("Updated Collection");
                    when(request.groupId()).thenReturn("group-1");
                    when(mapper.readValue("{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}", CreateCollectionRequest.class)).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(collectionMock.getName()).thenReturn("Updated Collection");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);
                    when(collectionMock.getCoins()).thenReturn(List.of());
                    String responseJson = "{" +
                        "\"id\":\"" + VALID_UUID + "\"," +
                        "\"name\":\"Updated Collection\"," +
                        "\"groupId\":\"group-1\"," +
                        "\"coins\":[]}";
                    when(mapper.writeValueAsString(any())).thenReturn(responseJson);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                "PATCH: Happy path, updates collection"
            ),
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    when(mapper.readValue("{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}", CreateCollectionRequest.class)).thenThrow(new JacksonException("fail"){});
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "PATCH: JacksonException during deserialization -> 400"
            ),
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.name()).thenReturn("Updated Collection");
                    when(request.groupId()).thenReturn("group-1");
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doThrow(new EuroCoinCollectionSaveException("fail")).when(collectionService).updateMetadata(any(EuroCoinCollection.class));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: EuroCoinCollectionSaveException -> 500"
            )
            ,
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    doThrow(new EuroCoinCollectionGroupGetByIdException("fail")).when(groupService).getById("group-1");
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: EuroCoinCollectionGroupGetByIdException -> 500"
            )
            ,
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Parent resource not found\"}",
                "PATCH: EuroCoinCollectionGroupNotFoundException -> 404"
            )
            ,
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.name()).thenReturn("Updated Collection");
                    when(request.groupId()).thenReturn("group-1");
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(collectionMock.getName()).thenReturn("Updated Collection");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);
                    when(collectionMock.getCoins()).thenReturn(List.of());
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail"){});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: JacksonException during serialization -> 500"
            ),
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(mapper.readValue(any(String.class), eq(CreateCollectionRequest.class))).thenReturn(request);
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: EuroCoinCollectionCoinsLoadException -> 500"
            ),
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(mapper.readValue("{\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}", CreateCollectionRequest.class)).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other user");
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: direct owner check failed"
            ),
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"new groupId\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.groupId()).thenReturn("new groupId");
                    when(mapper.readValue("{\"name\":\"Updated Collection\",\"groupId\":\"new groupId\",\"coins\":[]}", CreateCollectionRequest.class)).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    var groupMock2 = mock(EuroCoinCollectionGroup.class);
                    when(groupMock2.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupService.getById("new groupId")).thenReturn(groupMock2);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: owner check for new group failed"
            ),
            new CollectionHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"Updated Collection\",\"groupId\":\"new groupId\",\"coins\":[]}",
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var request = mock(CreateCollectionRequest.class);
                    when(request.groupId()).thenReturn("new groupId");
                    when(mapper.readValue("{\"name\":\"Updated Collection\",\"groupId\":\"new groupId\",\"coins\":[]}", CreateCollectionRequest.class)).thenReturn(request);
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    var groupMock2 = mock(EuroCoinCollectionGroup.class);
                    when(groupMock2.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupService.getById("new groupId")).thenReturn(groupMock2);
                    when(collectionMock.getName()).thenReturn("Updated Collection");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);
                    when(collectionMock.getCoins()).thenReturn(List.of());
                    String responseJson = "{" +
                        "\"id\":\"" + VALID_UUID + "\"," +
                        "\"name\":\"Updated Collection\"," +
                        "\"groupId\":\"new groupId\"," +
                        "\"coins\":[]}";
                    when(mapper.writeValueAsString(any())).thenReturn(responseJson);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"name\":\"Updated Collection\",\"groupId\":\"new groupId\",\"coins\":[]}",
                "Patch: happy path with new groupId"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doNothing().when(collectionService).delete(VALID_UUID);
                },
                204,
                null,
                "DELETE: Happy path, deletes collection successfully"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: EuroCoinCollectionNotFoundException -> 404"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: EuroCoinCollectionGroupNotFoundException -> 404"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doThrow(new EuroCoinCollectionDeleteException("fail")).when(collectionService).delete(VALID_UUID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: EuroCoinCollectionDeleteException -> 500"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    doThrow(new EuroCoinCollectionGetByIdException("fail")).when(collectionService).getById(VALID_UUID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: EuroCoinCollectionGetByIdException -> 500"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    doThrow(new EuroCoinCollectionGroupGetByIdException("fail")).when(groupService).getById("group-1");
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: EuroCoinCollectionGroupGetByIdException -> 500"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    doThrow(new EuroCoinCollectionCoinsLoadException("fail")).when(collectionService).getById(VALID_UUID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: EuroCoinCollectionCoinsLoadException -> 500"
            ),
            new CollectionHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (collectionService, groupService, mapper) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: Owner check failed, returns 404"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("collectionHandleTestcases")
    void testHandle(CollectionHandleTestcase testcase) throws IOException {
        var collectionService = mock(EuroCoinCollectionStorageService.class);
        var groupService = mock(EuroCoinCollectionGroupStorageService.class);
        var mapper = mock(ObjectMapper.class);
        CollectionHandler handler = new CollectionHandler(collectionService, groupService, mapper);
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
            testcase.mockSetup().setup(collectionService, groupService, mapper);
        } catch (Exception e) {
            fail("fail due to unexcpected exception in setup", e);
        }

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(testcase.expectedStatus), anyLong());

        if (testcase.expectedResponseBody != null) {
            String actual = responseStream.toString();
            assertEquals(testcase.expectedResponseBody, actual);
        }
    }
}
