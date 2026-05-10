package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinSaveException;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinDescription;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.Mint;
import io.github.lstramke.coincollector.model.DTOs.Requests.CoinActionRequest;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;
import io.github.lstramke.coincollector.services.EuroCoinCollectionStorageService;
import io.github.lstramke.coincollector.services.EuroCoinStorageService;
import io.github.lstramke.coincollector.services.SessionManager;
import io.github.lstramke.security.SecurityConfig;
import jakarta.servlet.http.Cookie;

@WebMvcTest(value = CoinHandler.class)
@Import(SecurityConfig.class)
public class CoinHandlerTest {

    @MockitoBean
    EuroCoinStorageService coinService;

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
            EuroCoinStorageService coinService, 
            EuroCoinCollectionStorageService collectionService, 
            EuroCoinCollectionGroupStorageService groupService,
            SessionManager sessionManager
        ) throws Exception;
    }

    private static final String PREFIX = "/api/v1/coins";
    private static final String USER_ID = "user-1";
    private static final String VALID_ID = "mock-id";
    private static final String SESSION_ID = "session-abc";

    private record CoinHandlerTestcase(
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

    private static EuroCoin createMockCoin(
        String id, 
        int year, 
        CoinValue value, 
        CoinCountry country, 
        Mint mint, 
        String collectionId, 
        String description
    ) {
        EuroCoin mockCoin = mock(EuroCoin.class);
        when(mockCoin.getId()).thenReturn(id);
        when(mockCoin.getYear()).thenReturn(year);
        when(mockCoin.getValue()).thenReturn(value);
        when(mockCoin.getMintCountry()).thenReturn(country);
        when(mockCoin.getMint()).thenReturn(mint);
        when(mockCoin.getDescription()).thenReturn(new CoinDescription(description));
        when(mockCoin.getCollectionId()).thenReturn(collectionId);
        return mockCoin;
    }

    private record CoinHandlerGetDeleteTestcase(
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
    
    private static Stream<CoinHandlerGetDeleteTestcase> coinGetTestcases() {
        return Stream.of(
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "GET: unauthorized session"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "{\"id\":\"" + VALID_ID + "\",\"year\":2002,\"value\":100,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"description\"}",
                "GET: happy path"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: owner check fails"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    when(coinService.getById(VALID_ID)).thenThrow(new EuroCoinNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: coin not found (EuroCoinNotFoundException)"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: collection not found (EuroCoinCollectionNotFoundException)"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: group not found (EuroCoinCollectionGroupNotFoundException )"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGetByIdException"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGroupGetByIdException"
            ),
            new CoinHandlerGetDeleteTestcase(
                "GET",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionCoinsLoadException"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("coinGetTestcases")
    void testCoinHandlerGet(CoinHandlerGetDeleteTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(coinService, collectionService, groupService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            get(testcase.path)
            .cookie(new Cookie("sessionId", SESSION_ID))
        ).andExpect(status().is(testcase.expectedStatus));

        if(testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
            if (testcase.expectedStatus == 401) {
                verify(coinService, times(0)).getById(anyString());
            } else {
                verify(coinService, times(1)).getById(anyString());
            }
        }
    }

    private static Stream<CoinHandlerTestcase> coinCreateTestcases() {
        return Stream.of(
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "test coin",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "POST: unauthorized session"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "test coin",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2022);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).save(any(EuroCoin.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                201, 
                "{\"id\":\"GERMANY_ONE_EURO_2022_BERLIN\",\"year\":2022,\"value\":100,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"test coin\"}", 
                "POST: happy path"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 50,
                    "country": "FR",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2022);
                    when(request.value()).thenReturn(50);
                    when(request.country()).thenReturn("FR");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn(null);
                    when(request.mint()).thenReturn(null);
                
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).save(any(EuroCoin.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                201, 
                "{\"id\":\"FRANCE_FIFTY_CENTS_2022_UNKOWN\",\"year\":2022,\"value\":50,\"country\":\"FR\",\"collectionId\":\"collection-1\",\"mint\":null,\"description\":\"50 Cent Münze aus Frankreich aus dem Jahr 2022\"}", 
                "POST: happy path with non-german coin, no description, no mint"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                404, 
                "{\"error\":\"Resource not found\"}",
                "POST: owner check fails"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);

                }, 
                404, 
                "{\"error\":\"Parent resource not found\"}",
                "POST: collection for coin doesnt exists (EuroCoinCollectionNotFoundException)"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                404, 
                "{\"error\":\"Parent resource not found\"}",
                "POST: group for collection doesnt exists (EuroCoinCollectionGroupNotFoundException)"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """, 
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinAlreadyExistsException("already exists")).when(coinService).save(any(EuroCoin.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                409, 
                "{\"error\":\"Coin already exists\"}", 
                "POST: coin already exists (EuroCoinAlreadyExistsException)"
            ),
            new CoinHandlerTestcase(
                 "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);

                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: exception retriving collection for coin (EuroCoinCollectionGetByIdException)"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: exception retriving group for collection for coin (EuroCoinCollectionGroupGetByIdException)"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """, 
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinSaveException("fail")).when(coinService).save(any(EuroCoin.class));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}", 
                "POST: error saving coin (EuroCoinSaveException)"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "not existing mint",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("not existing mint");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: error in mint translation because mint is not existing (IllegalArgumentException)"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 0,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(0);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: error in coin builder because year is to small (IllegalStateException)"
            ),
            new CoinHandlerTestcase(
                 "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);

                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: exception retriving coins in collection for coin (EuroCoinCollectionCoinsLoadException)"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("coinCreateTestcases")
    void testCoinHandlerPost(CoinHandlerTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(coinService, collectionService, groupService, sessionManager);
        } catch (Exception e) {
            fail(" due to unexcpected exception in setup");
        }

        var actionResult = mockMvc.perform(
            post(testcase.path)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(new Cookie("sessionId", SESSION_ID))
            .content(testcase.requestBody)
        ).andExpect(status().is(testcase.expectedStatus));

        if(testcase.expectedResponseBody != null) {
            actionResult.andExpect(content().json(testcase.expectedResponseBody));
        }

        if(testcase.expectedStatus == 201) {
            verify(coinService, times(1)).save(any(EuroCoin.class));
        }
    }

    private static Stream<CoinHandlerTestcase> coinUpdateTestcases(){
        return Stream.of(
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "PATCH: unauthorized session"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_ID);
                    doNothing().when(coinService).save(any());
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "{\"id\":\"GERMANY_TWO_EUROS_2023_BERLIN\",\"year\":2023,\"value\":200,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"Updated description\"}",
                "PATCH: happy path"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "FR",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("FR");
                    when(request.collectionId()).thenReturn("collection-1");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_ID);
                    doNothing().when(coinService).save(any());
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "{\"id\":\"FRANCE_TWO_EUROS_2023_UNKOWN\",\"year\":2023,\"value\":200,\"country\":\"FR\",\"collectionId\":\"collection-1\",\"mint\":null,\"description\":\"2 Euro Münze aus Frankreich aus dem Jahr 2023\"}",
                "PATCH: happy path, non german coin without description"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: direct owner check fails"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "new-collection"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("new-collection");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    var mockCollection2 = mock(EuroCoinCollection.class);
                    when(mockCollection2.getGroupId()).thenReturn("group-2");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(collectionService.getById("new-collection")).thenReturn(mockCollection2);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    var mockGroup2 = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup2.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(groupService.getById("group-2")).thenReturn(mockGroup2);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: owner check fails for new collection"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("new-collection");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    var mockCollection2 = mock(EuroCoinCollection.class);
                    when(mockCollection2.getGroupId()).thenReturn("group-2");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(collectionService.getById("new-collection")).thenReturn(mockCollection2);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    var mockGroup2 = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup2.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(groupService.getById("group-2")).thenReturn(mockGroup2);
                    doNothing().when(coinService).delete(VALID_ID);
                    doNothing().when(coinService).save(any());
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                200,
                "{\"id\":\"GERMANY_TWO_EUROS_2023_BERLIN\",\"year\":2023,\"value\":200,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"Updated description\"}",
                "PATCH: happy path with two successful owner checks"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    when(coinService.getById(VALID_ID)).thenThrow(new EuroCoinNotFoundException("not found"));
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: coin to update doesnt exists"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: collection for coin not found (EuroCoinCollectionNotFoundException)"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: group for collection for coin not found (EuroCoinCollectionGroupNotFoundException)"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception retriving collection for coin (EuroCoinCollectionGetByIdException)"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception retriving collection for coin (EuroCoinCollectionCoinsLoadException)"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: group for collection for coin not found (EuroCoinCollectionGroupGetByIdException)"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinDeleteException("fail")).when(coinService).delete(VALID_ID);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception during delete of old coin"
            ),
            new CoinHandlerTestcase(
                "PATCH",
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "DE",
                    "mint": "A",
                    "description": "Updated description",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_ID);
                    doThrow(new EuroCoinSaveException("fail")).when(coinService).save(any());
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception during save of updated coin"
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX + "/" + VALID_ID, 
                """
                {
                    "year": 2022,
                    "value": 100,
                    "country": "DE",
                    "mint": "not existing mint",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("not existing mint");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "PATCH: error in mint translation because mint is not existing "
            ),
            new CoinHandlerTestcase(
                "POST", 
                PREFIX + "/" + VALID_ID,
                """
                {
                    "year": 0,
                    "value": 100,
                    "country": "DE",
                    "mint": "A",
                    "description": "Testmünze",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, sessionManager) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(0);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "PATCH: error in coin builder because year is to small (IllegalStateException)"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("coinUpdateTestcases")
    void testCoinHandlerPatch(CoinHandlerTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(coinService, collectionService, groupService, sessionManager);
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

        if (testcase.expectedStatus == 401) {
            verify(coinService, times(0)).getById(anyString());
        } else {
            verify(coinService).getById(VALID_ID);

            if (testcase.expectedStatus == 200) {
                verify(coinService, times(1)).delete(VALID_ID);
                verify(coinService, times(1)).save(any(EuroCoin.class));
            }
        }
    }

    private static Stream<CoinHandlerGetDeleteTestcase> coinDeleteTestcases(){
        return Stream.of(
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(false);
                },
                401,
                "{\"error\":\"Unauthorized\"}",
                "DELETE: unauthorized session"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_ID);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                204,
                null,
                "DELETE: happy path"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    when(coinService.getById(VALID_ID)).thenThrow(new EuroCoinNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: coin to delete not found"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: collection for coin not found"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: group of collection of coin not found"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: exception retriving collection for coin (EuroCoinCollectionGetByIdException)"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: exception retriving collection for coin (EuroCoinCollectionCoinsLoadException)"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: exception retriving group of collection of coin"
            ),
            new CoinHandlerGetDeleteTestcase(
                "DELETE",
                PREFIX + "/" + VALID_ID,
                USER_ID,
                (coinService, collectionService, groupService, sessionManager) -> {
                    var mockCoin = createMockCoin(
                        VALID_ID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_ID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinDeleteException("fail")).when(coinService).delete(VALID_ID);
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: deleting coin fails"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("coinDeleteTestcases")
    void testCoinHandlerDelete(CoinHandlerGetDeleteTestcase testcase) throws Exception {
        try {
            testcase.mockSetup.setup(coinService, collectionService, groupService, sessionManager);
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
            verify(coinService, times(0)).getById(anyString());
        } else {
            verify(coinService).getById(VALID_ID);

            if (testcase.expectedStatus == 204) {
                verify(coinService, times(1)).delete(VALID_ID);
            } else if(testcase.expectedStatus != 500) {
                verify(coinService, times(0)).delete(VALID_ID);
            }
        }

    }
}
