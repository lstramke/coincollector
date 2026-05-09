package io.github.lstramke.coincollector.handler;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupUpdateException;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.DTOs.Requests.UpdateGroupRequest;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import io.github.lstramke.coincollector.services.SessionManager;
import io.github.lstramke.security.SecurityConfig;

@WebMvcTest(value = GroupHandler.class)
@Import(SecurityConfig.class)
class GroupHandlerTest {

    @MockitoBean
    SessionManager sessionManager;

    @MockitoBean
    EuroCoinCollectionGroupStorageService groupStorageService;

    @Autowired
    MockMvc mockMvc;

    @FunctionalInterface
    interface MockSetup {
        void setup(EuroCoinCollectionGroupStorageService service, SessionManager sessionManager) throws Exception;
    }

    private static final String PREFIX = "/api/v1/groups";
    private static final String USER_ID = "user-1";
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String SESSION_ID = "session-abc";

    private record GroupHandlerDeleteGetTestcase(
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

    private static Stream<GroupHandlerDeleteGetTestcase> deleteTestcases() {
        return Stream.of(
            new GroupHandlerDeleteGetTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn(VALID_UUID);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doNothing().when(service).delete(VALID_UUID);

                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                204,
                null,
                "Success case: DELETE group returns 204"
            ),
            new GroupHandlerDeleteGetTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn(VALID_UUID);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE group fails owner check and returns 404"
            ),
            new GroupHandlerDeleteGetTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn(VALID_UUID);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doThrow(new EuroCoinCollectionGroupDeleteException("fail")).when(service).delete(VALID_UUID);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE group triggers delete exception and returns 500"
            ),
            new GroupHandlerDeleteGetTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE group triggers not found exception and returns 404"
            ),
            new GroupHandlerDeleteGetTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "DELETE unauthenticated returns 401"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testGroupHandlerDelete(GroupHandlerDeleteGetTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(groupStorageService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            delete(testcase.path)
            .cookie(new Cookie("sessionId", SESSION_ID))
        ).andExpect(status().is(testcase.expectedStatus));

        if(testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }

        if (testcase.expectedStatus == 401) {
            verify(groupStorageService, never()).getById(VALID_UUID);
            verify(groupStorageService, never()).delete(VALID_UUID);
        } else {
            verify(groupStorageService).getById(VALID_UUID);
            if (testcase.expectedStatus == 204) {
                verify(groupStorageService).delete(VALID_UUID);
            } else if(testcase.expectedStatus != 500) {
                verify(groupStorageService, never()).delete(VALID_UUID);
            }
        }
    }

    private static Stream<GroupHandlerDeleteGetTestcase> getTestcases() {
        return Stream.of(
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX,
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn("id-1");
                    when(groupMock.getName()).thenReturn("test group");
                    when(groupMock.getCollections()).thenReturn(java.util.List.of());
                    when(service.getAllByUser(USER_ID)).thenReturn(java.util.List.of(groupMock));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "[{\"id\":\"id-1\",\"name\":\"test group\",\"collections\":[]}]",
                "GET all groups returns 200 and JSON array"
            ),
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getId()).thenReturn(VALID_UUID);
                    when(groupMock.getName()).thenReturn("test group");
                    when(groupMock.getCollections()).thenReturn(java.util.List.of());
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"name\":\"test group\",\"collections\":[]}",
                "GET by ID returns 200 and JSON object"
            ),
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX,
                USER_ID,
                (service, sessionManager) -> {
                    when(service.getAllByUser(USER_ID)).thenThrow(new EuroCoinCollectionGroupGetAllException());
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET all groups triggers GetAllException and returns 500"
            ),
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET by ID triggers NotFoundException and returns 404"
            ),
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupGetByIdException(VALID_UUID));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET by ID triggers GetByIdException and returns 500"
            ),
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET by ID fails owner check and returns 404"
            ),
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX,
                USER_ID,
                (service, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "GET all unauthenticated returns 401"
            ),
            new GroupHandlerDeleteGetTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                USER_ID,
                (service, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "GET by ID unauthenticated returns 401"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getTestcases")
    void testGroupHandlerGet(GroupHandlerDeleteGetTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(groupStorageService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            get(testcase.path)
            .cookie(new Cookie("sessionId", SESSION_ID))
        ).andExpect(status().is(testcase.expectedStatus));

        if(testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }

        if (testcase.path.equals(PREFIX)) {
            if (testcase.expectedStatus == 401) {
                verify(groupStorageService, never()).getAllByUser(USER_ID);
            } else {
                verify(groupStorageService).getAllByUser(USER_ID);
            }
        } else {
            if (testcase.expectedStatus == 401) {
                verify(groupStorageService, never()).getById(VALID_UUID);
            } else {
                verify(groupStorageService).getById(VALID_UUID);
            }
        }
    }

    private record GroupHandlerPostPatchTestcase(
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

    private static Stream<GroupHandlerPostPatchTestcase> postTestcases() {
        return Stream.of(
            new GroupHandlerPostPatchTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"test group\",\"collectionIds\":[]}",
                USER_ID,
                (service, sessionManager) -> {
                    doNothing().when(service).save(any(EuroCoinCollectionGroup.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                201,
                null,
                "POST create group: Happy Path"
            ),
            new GroupHandlerPostPatchTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"test group\"}",
                USER_ID,
                (service, sessionManager) -> {
                    doThrow(new EuroCoinCollectionGroupSaveException("fail")).when(service).save(any(EuroCoinCollectionGroup.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "POST create group: EuroCoinCollectionGroupSaveException (500)"
            ),
            new GroupHandlerPostPatchTestcase(
                "POST",
                PREFIX,
                "{invalid json}",
                USER_ID,
                (service, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "POST invalid JSON returns 400"
            ),
            new GroupHandlerPostPatchTestcase(
                "POST",
                PREFIX,
                "{\"name\":\"test group\",\"collectionIds\":[]}",
                USER_ID,
                (service, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "POST unauthenticated returns 401"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("postTestcases")
    void testGrouphandlerPost(GroupHandlerPostPatchTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(groupStorageService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            post(testcase.path)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("sessionId", SESSION_ID))
            .content(testcase.requestBody)
        ).andExpect(status().is(testcase.expectedStatus));

        if (testcase.expectedStatus == 201) {
            actionResult
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("test group"))
                .andExpect(jsonPath("$.collections").isArray());
            verify(groupStorageService).save(any(EuroCoinCollectionGroup.class));
        } else if (testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }

        if (testcase.expectedStatus == 201 || testcase.expectedStatus == 500) {
            verify(groupStorageService).save(any(EuroCoinCollectionGroup.class));
        } else {
            verify(groupStorageService, never()).save(any(EuroCoinCollectionGroup.class));
        }

    }

    private static Stream<GroupHandlerPostPatchTestcase> patchTestcases() {
        return Stream.of(
            new GroupHandlerPostPatchTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, sessionManager) -> {
                    var updateRequest = mock(UpdateGroupRequest.class);
                    when(updateRequest.name()).thenReturn("new group name");
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(groupMock.getName()).thenReturn("new group name");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doNothing().when(service).updateMetadata(groupMock);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "{\"name\":\"new group name\"}",
                "PATCH update group: Happy Path"
            ),
            new GroupHandlerPostPatchTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, sessionManager) -> {
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH update group: NotFoundException (404)"
            ),
            new GroupHandlerPostPatchTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, sessionManager) -> {
                    when(service.getById(VALID_UUID)).thenThrow(new EuroCoinCollectionGroupGetByIdException(VALID_UUID));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH update group: GetByIdException (500)"
            ),
            new GroupHandlerPostPatchTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn(USER_ID);
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    doThrow(new EuroCoinCollectionGroupUpdateException("fail")).when(service).updateMetadata(groupMock);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH update group: UpdateException (500)"
            ),
            new GroupHandlerPostPatchTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, sessionManager) -> {
                    var groupMock = mock(EuroCoinCollectionGroup.class);
                    when(groupMock.getOwnerId()).thenReturn("other-user");
                    when(service.getById(VALID_UUID)).thenReturn(groupMock);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH update group: Owner check fails (404)"
            ),
            new GroupHandlerPostPatchTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{invalid json}",
                USER_ID,
                (service, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                400,
                "{\"error\":\"Request is not valid\"}",
                "PATCH invalid JSON returns 400"
            ),
            new GroupHandlerPostPatchTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                "{\"name\":\"new group name\"}",
                USER_ID,
                (service, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "PATCH unauthenticated returns 401"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("patchTestcases")
    void testGrouphandlerPatch(GroupHandlerPostPatchTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(groupStorageService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            patch(testcase.path)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("sessionId", SESSION_ID))
            .content(testcase.requestBody)
        ).andExpect(status().is(testcase.expectedStatus));

        if(testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }
        
        if (testcase.expectedStatus == 200) {
            verify(groupStorageService).getById(VALID_UUID);
            verify(groupStorageService).updateMetadata(any(EuroCoinCollectionGroup.class));
        } else if(testcase.expectedStatus == 400 || testcase.expectedStatus == 401) {
            verify(groupStorageService, never()).getById(VALID_UUID);
            verify(groupStorageService, never()).updateMetadata(any(EuroCoinCollectionGroup.class));
        }

    }
}
