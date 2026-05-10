package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionCoinsLoadException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.security.SecurityConfig;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import io.github.lstramke.coincollector.services.EuroCoinCollectionStorageService;
import io.github.lstramke.coincollector.services.SessionManager;
import jakarta.servlet.http.Cookie;

@WebMvcTest(value = CollectionHandler.class)
@Import(SecurityConfig.class)
class CollectionHandlerTest {

    @MockitoBean
    EuroCoinCollectionStorageService collectionService;

    @MockitoBean
    EuroCoinCollectionGroupStorageService groupService;

    @MockitoBean
    SessionManager sessionManager;

    @Autowired
    MockMvc mockMvc;

    @FunctionalInterface
    interface MockSetup {
        void setup(
            EuroCoinCollectionStorageService collectionService,
            EuroCoinCollectionGroupStorageService groupService,
            SessionManager sessionManager
        ) throws Exception;
    }

    private static final String PREFIX = "/api/v1/collections";
    private static final String USER_ID = "user-1";
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SESSION_ID = "session-abc";

    private record CollectionHandlerGetDeleteTestcase(
        String method,
        String path,
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

    private record CollectionHandlerTestcase(
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

    private static Stream<CollectionHandlerGetDeleteTestcase> collectionGetTestcases() {
        return Stream.of(
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "GET: unauthorized session"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collection = mock(EuroCoinCollection.class);
                    when(collection.getGroupId()).thenReturn("group-1");
                    when(collection.getName()).thenReturn("German Euro Coins");
                    when(collection.getId()).thenReturn(VALID_UUID);
                    when(collection.getCoins()).thenReturn(List.of());
                    when(collectionService.getById(VALID_UUID)).thenReturn(collection);

                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                null,
                "GET: happy path"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);

                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other user");

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: owner check fails"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: EuroCoinCollectionNotFoundException -> 404"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collection = new EuroCoinCollection("German Euro Coins", "group-1");
                    when(collectionService.getById(VALID_UUID)).thenReturn(collection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: EuroCoinCollectionGroupNotFoundException -> 404"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collection = mock(EuroCoinCollection.class);
                    when(collection.getGroupId()).thenReturn("group-1");
                    when(collection.getName()).thenReturn("German Euro Coins");
                    when(collection.getId()).thenReturn(VALID_UUID);
                    when(collection.getCoins()).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                    when(collectionService.getById(VALID_UUID)).thenReturn(collection);

                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionCoinsLoadException -> 500"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGetByIdException -> 500"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collection = new EuroCoinCollection("German Euro Coins", "group-1");
                    when(collectionService.getById(VALID_UUID)).thenReturn(collection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGroupGetByIdException -> 500"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("collectionGetTestcases")
    void testCollectionHandlerGet(CollectionHandlerGetDeleteTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(collectionService, groupService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            get(testcase.path)
            .cookie(new Cookie("sessionId", SESSION_ID))
        ).andExpect(status().is(testcase.expectedStatus));

        if (testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }
    }

    private static Stream<CollectionHandlerTestcase> collectionCreateTestcases() {
        return Stream.of(
            new CollectionHandlerTestcase(
                "POST",
                PREFIX,
                """
                {
                    "name": "German Euro Coins",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "POST: unauthorized session"
            ),
            new CollectionHandlerTestcase(
                "POST",
                PREFIX,
                """
                {
                    "name": "German Euro Coins",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                201,
                null,
                "POST: happy path, creates collection"
            ),
            new CollectionHandlerTestcase(
                "POST",
                PREFIX,
                """
                {
                    "name": "German Euro Coins",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other user");
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "POST: owner check fails"
            ),
            new CollectionHandlerTestcase(
                "POST",
                PREFIX,
                "{invalid-json",
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "POST: JacksonException during deserialization -> 400"
            ),
            new CollectionHandlerTestcase(
                "POST",
                PREFIX,
                """
                {
                    "name": "German Euro Coins",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doThrow(new EuroCoinCollectionGroupGetByIdException("fail")).when(collectionService).save(any(EuroCoinCollection.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST: EuroCoinCollectionGroupGetByIdException -> 500"
            ),
            new CollectionHandlerTestcase(
                "POST",
                PREFIX,
                """
                {
                    "name": "German Euro Coins",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Parent resource not found\"}",
                "POST: EuroCoinCollectionGroupNotFoundException -> 404"
            ),
            new CollectionHandlerTestcase(
                "POST",
                PREFIX,
                """
                {
                    "name": "German Euro Coins",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doThrow(new EuroCoinCollectionSaveException("fail")).when(collectionService).save(any(EuroCoinCollection.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST: EuroCoinCollectionSaveException -> 500"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("collectionCreateTestcases")
    void testCollectionHandlerPost(CollectionHandlerTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(collectionService, groupService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            post(testcase.path)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("sessionId", SESSION_ID))
            .content(testcase.requestBody)
        ).andExpect(status().is(testcase.expectedStatus));

        if (testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }

        if (testcase.expectedStatus == 201) {
            actionResult
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("German Euro Coins"))
                .andExpect(jsonPath("$.groupId").value("group-1"))
                .andExpect(jsonPath("$.coins").isArray());
            verify(collectionService, times(1)).save(any(EuroCoinCollection.class));
        }
    }

    private static Stream<CollectionHandlerTestcase> collectionUpdateTestcases() {
        return Stream.of(
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "PATCH: unauthorized session"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionMock.getName()).thenReturn("Updated Collection");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);
                    when(collectionMock.getCoins()).thenReturn(List.of());

                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);

                    doNothing().when(collectionService).updateMetadata(any(EuroCoinCollection.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"name\":\"Updated Collection\",\"groupId\":\"group-1\",\"coins\":[]}",
                "PATCH: happy path"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{invalid-json",
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "PATCH: JacksonException during deserialization -> 400"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionMock.getCoins()).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));

                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: EuroCoinCollectionCoinsLoadException -> 500"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: EuroCoinCollectionGetByIdException -> 500"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1", "new groupId");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Parent Resource not found\"}",
                "PATCH: EuroCoinCollectionGroupNotFoundException -> 404"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "new groupId",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionMock.getCoins()).thenReturn(List.of());

                    var currentGroup = mock(EuroCoinCollectionGroup.class);
                    when(currentGroup.getOwnerId()).thenReturn(USER_ID);
                    var newGroup = mock(EuroCoinCollectionGroup.class);
                    when(newGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(currentGroup);
                    when(groupService.getById("new groupId")).thenReturn(newGroup);

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: owner check for new group failed"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "new groupId",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1", "new groupId");
                    when(collectionMock.getCoins()).thenReturn(List.of());
                    when(collectionMock.getName()).thenReturn("Updated Collection");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);

                    var currentGroup = mock(EuroCoinCollectionGroup.class);
                    when(currentGroup.getOwnerId()).thenReturn(USER_ID);
                    var newGroup = mock(EuroCoinCollectionGroup.class);
                    when(newGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(currentGroup);
                    when(groupService.getById("new groupId")).thenReturn(newGroup);

                    doNothing().when(collectionService).updateMetadata(any(EuroCoinCollection.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                null,
                "PATCH: happy path with new groupId"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionMock.getCoins()).thenReturn(List.of());

                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other user");

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: direct owner check fails"
            ),
            new CollectionHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "name": "Updated Collection",
                    "groupId": "group-1",
                    "coins": []
                }
                """,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(collectionMock.getCoins()).thenReturn(List.of());
                    when(collectionMock.getName()).thenReturn("Updated Collection");
                    when(collectionMock.getId()).thenReturn(VALID_UUID);

                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);

                    doThrow(new EuroCoinCollectionSaveException("fail")).when(collectionService).updateMetadata(any(EuroCoinCollection.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: EuroCoinCollectionSaveException -> 500"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("collectionUpdateTestcases")
    void testCollectionHandlerPatch(CollectionHandlerTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(collectionService, groupService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            patch(testcase.path)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("sessionId", SESSION_ID))
            .content(testcase.requestBody)
        ).andExpect(status().is(testcase.expectedStatus));

        if (testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        } else if (testcase.expectedStatus == 200) {
            actionResult
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Updated Collection"))
                .andExpect(jsonPath("$.groupId").value(testcase.requestBody.contains("new groupId") ? "new groupId" : "group-1"))
                .andExpect(jsonPath("$.coins").isArray());
        }
    }

    private static Stream<CollectionHandlerGetDeleteTestcase> collectionDeleteTestcases() {
        return Stream.of(
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "DELETE: unauthorized session"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collection = new EuroCoinCollection("German Euro Coins", "group-1");
                    when(collectionService.getById(VALID_UUID)).thenReturn(collection);
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doNothing().when(collectionService).delete(VALID_UUID);

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                204,
                null,
                "DELETE: happy path"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: EuroCoinCollectionNotFoundException -> 404"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: EuroCoinCollectionGroupNotFoundException -> 404"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    when(collectionService.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: EuroCoinCollectionGetByIdException -> 500"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: EuroCoinCollectionGroupGetByIdException -> 500"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: Owner check failed, returns 404"
            ),
            new CollectionHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (collectionService, groupService, sessionManager) -> {
                    var collectionMock = mock(EuroCoinCollection.class);
                    when(collectionService.getById(VALID_UUID)).thenReturn(collectionMock);
                    when(collectionMock.getGroupId()).thenReturn("group-1");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupService.getById("group-1")).thenReturn(groupMock);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    doThrow(new EuroCoinCollectionDeleteException("fail")).when(collectionService).delete(VALID_UUID);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: EuroCoinCollectionDeleteException -> 500"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("collectionDeleteTestcases")
    void testCollectionHandlerDelete(CollectionHandlerGetDeleteTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(collectionService, groupService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            delete(testcase.path)
            .cookie(new Cookie("sessionId", SESSION_ID))
        ).andExpect(status().is(testcase.expectedStatus));

        if (testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }

        if (testcase.expectedStatus == 204) {
            verify(collectionService, times(1)).delete(VALID_UUID);
        }
    }
}
