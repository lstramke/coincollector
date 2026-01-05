package io.github.lstramke.coincollector.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class CoinHandlerTest {
    
    @FunctionalInterface
    interface MockSetup {
        void setup(EuroCoinStorageService coinService, EuroCoinCollectionStorageService collectionService, EuroCoinCollectionGroupStorageService groupService, ObjectMapper mapper) throws Exception;
    }

    private static final String PREFIX = "/api/coins";
    private static final String USER_ID = "user-1";
    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";

    private record CoinHandleTestcase(
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

    
    private static Stream<CoinHandleTestcase> coinGetTestcases() {
        return Stream.of(
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    var expectedResponse = "{" +
                    "\"id\":\"" + VALID_UUID + "\"," +
                    "\"year\":2002," +
                    "\"value\":100," +
                    "\"country\":\"DE\"," +
                    "\"collectionId\":\"collection-1\"," +
                    "\"mint\":\"A\"," +
                    "\"description\":\"description\"}";
                    when(mapper.writeValueAsString(any())).thenReturn(expectedResponse);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"year\":2002,\"value\":100,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"description\"}",
                "GET: happy path"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: owner check fails"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    when(coinService.getById(VALID_UUID)).thenThrow(new EuroCoinNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: coin not found (EuroCoinNotFoundException)"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: collection not found (EuroCoinCollectionNotFoundException)"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID, 
                        2002, 
                        CoinValue.ONE_EURO, 
                        CoinCountry.GERMANY, 
                        Mint.BERLIN, 
                        "collection-1", 
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "GET: group not found (EuroCoinCollectionGroupNotFoundException )"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail") {});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: JacksonException"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGetByIdException"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionGroupGetByIdException"
            ),
            new CoinHandleTestcase(
                "GET",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "GET: EuroCoinCollectionCoinsLoadException"
            )
        );
    }

    private static Stream<CoinHandleTestcase> coinCreateTestcases() {
        return Stream.of(
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).save(any(EuroCoin.class));
                    var expectedResponse = "{" +
                        "\"id\":\"generated-id\"," +
                        "\"year\":2002," +
                        "\"value\":100," +
                        "\"country\":\"DE\"," +
                        "\"collectionId\":\"collection-1\"," +
                        "\"mint\":\"A\"," +
                        "\"description\":\"test coin\"}";
                    when(mapper.writeValueAsString(any())).thenReturn(expectedResponse);
                }, 
                201, 
                "{\"id\":\"generated-id\",\"year\":2002,\"value\":100,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"test coin\"}", 
                "POST: happy path"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2022);
                    when(request.value()).thenReturn(50);
                    when(request.country()).thenReturn("FR");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn(null);
                    when(request.mint()).thenReturn(null);
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 50,
                            "country": "FR",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);
                
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).save(any(EuroCoin.class));
                    var expectedResponse = "{" +
                        "\"id\":\"generated-id\"," +
                        "\"year\":2022," +
                        "\"value\":50," +
                        "\"country\":\"FR\"," +
                        "\"collectionId\":\"collection-1\"," +
                        "\"mint\":null," +
                        "\"description\":null}";
                    when(mapper.writeValueAsString(any())).thenReturn(expectedResponse);
                }, 
                201, 
                "{\"id\":\"generated-id\",\"year\":2022,\"value\":50,\"country\":\"FR\",\"collectionId\":\"collection-1\",\"mint\":null,\"description\":null}", 
                "POST: happy path with non-german coin, no description, no mint"
            ),
            new CoinHandleTestcase(
                "POST", 
                PREFIX, 
                """
                {
                    "year": 2022,
                    "value": 50,
                    "country": "FR",
                    "collectionId": "collection-1",
                    "wrong": "wrong"
                }
                """,
                USER_ID, 
                (coinService, collectionService, groupService, mapper) -> {
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 50,
                            "country": "FR",
                            "collectionId": "collection-1",
                            "wrong": "wrong"
                        }
                        """, CoinActionRequest.class)
                    ).thenThrow(new JacksonException("fail"){});

                }, 
                400, 
                "{\"error\":\"Invalid request body\"}", 
                "POST: JacksonException during binding of request"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                }, 
                404, 
                "{\"error\":\"Resource not found\"}",
                "POST: owner check fails"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));

                }, 
                404, 
                "{\"error\":\"Parent resource not found\"}",
                "POST: collection for coin doesnt exists (EuroCoinCollectionNotFoundException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                }, 
                404, 
                "{\"error\":\"Parent resource not found\"}",
                "POST: group for collection doesnt exists (EuroCoinCollectionGroupNotFoundException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinAlreadyExistsException("already exists")).when(coinService).save(any(EuroCoin.class));
                }, 
                409, 
                "{\"error\":\"Coin already exists\"}", 
                "POST: coin already exists (EuroCoinAlreadyExistsException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));

                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: exception retriving collection for coin (EuroCoinCollectionGetByIdException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: exception retriving group for collection for coin (EuroCoinCollectionGroupGetByIdException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinSaveException("fail")).when(coinService).save(any(EuroCoin.class));
                }, 
                500, 
                "{\"error\":\"Internal server error\"}", 
                "POST: error saving coin (EuroCoinSaveException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).save(any(EuroCoin.class));
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail"){});
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: error mapping response"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("not existing mint");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "not existing mint",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: error in mint translation because mint is not existing (IllegalArgumentException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(0);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue("""
                        {
                            "year": 0,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: error in coin builder because year is to small (IllegalStateException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));

                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "POST: exception retriving coins in collection for coin (EuroCoinCollectionCoinsLoadException)"
            )
        );
    }

    private static Stream<CoinHandleTestcase> coinUpdateTestcases(){
        return Stream.of(
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_UUID);
                    doNothing().when(coinService).save(any());
                    var expectedResponse = "{" +
                        "\"id\":\"" + VALID_UUID + "\"," +
                        "\"year\":2023," +
                        "\"value\":200," +
                        "\"country\":\"DE\"," +
                        "\"collectionId\":\"collection-1\"," +
                        "\"mint\":\"A\"," +
                        "\"description\":\"Updated description\"}";
                    when(mapper.writeValueAsString(any())).thenReturn(expectedResponse);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"year\":2023,\"value\":200,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"Updated description\"}",
                "PATCH: happy path"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
                """
                {
                    "year": 2023,
                    "value": 200,
                    "country": "FR",
                    "collectionId": "collection-1"
                }
                """,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("FR");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "FR",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_UUID);
                    doNothing().when(coinService).save(any());
                    var expectedResponse = "{" +
                        "\"id\":\"" + VALID_UUID + "\"," +
                        "\"year\":2023," +
                        "\"value\":200," +
                        "\"country\":\"FR\"," +
                        "\"collectionId\":\"collection-1\"," +
                        "\"mint\":\"\"," +
                        "\"description\":\"New description\"}";
                    when(mapper.writeValueAsString(any())).thenReturn(expectedResponse);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"year\":2023,\"value\":200,\"country\":\"FR\",\"collectionId\":\"collection-1\",\"mint\":\"\",\"description\":\"New description\"}",
                "PATCH: happy path, non german coin without description"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    when(mapper.readValue(eq(
                        """
                    {
                        "year": 2023,
                        "value": 200,
                        "country": "DE",
                        "mint": "A",
                        "description": "Updated description",
                        "collectionId": "collection-1"
                    }
                    """), eq(CoinActionRequest.class))
                    ).thenThrow(new JacksonException("fail"){});
                },
                400,
                "{\"error\":\"Invalid request body\"}",
                "PATCH: bining request failes"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: direct owner check fails"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("new-collection");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
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
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: owner check fails for new collection"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("new-collection");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
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
                    doNothing().when(coinService).delete(VALID_UUID);
                    doNothing().when(coinService).save(any());
                    var expectedResponse = "{" +
                        "\"id\":\"" + VALID_UUID + "\"," +
                        "\"year\":2023," +
                        "\"value\":200," +
                        "\"country\":\"DE\"," +
                        "\"collectionId\":\"collection-1\"," +
                        "\"mint\":\"A\"," +
                        "\"description\":\"Updated description\"}";
                    when(mapper.writeValueAsString(any())).thenReturn(expectedResponse);
                },
                200,
                "{\"id\":\"" + VALID_UUID + "\",\"year\":2023,\"value\":200,\"country\":\"DE\",\"collectionId\":\"collection-1\",\"mint\":\"A\",\"description\":\"Updated description\"}",
                "PATCH: happy path with two successful owner checks"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    when(coinService.getById(VALID_UUID)).thenThrow(new EuroCoinNotFoundException("not found"));
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: coin to update doesnt exists"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: collection for coin not found (EuroCoinCollectionNotFoundException)"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "PATCH: group for collection for coin not found (EuroCoinCollectionGroupNotFoundException)"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception retriving collection for coin (EuroCoinCollectionGetByIdException)"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception retriving collection for coin (EuroCoinCollectionCoinsLoadException)"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.collectionId()).thenReturn("collection-1");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: group for collection for coin not found (EuroCoinCollectionGroupGetByIdException)"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinDeleteException("fail")).when(coinService).delete(VALID_UUID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception during delete of old coin"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_UUID);
                    doThrow(new EuroCoinSaveException("fail")).when(coinService).save(any());
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception during save of updated coin"
            ),
            new CoinHandleTestcase(
                "PATCH",
                PREFIX + "/" + VALID_UUID,
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
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "old description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2023);
                    when(request.value()).thenReturn(200);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("Updated description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue(
                        eq("""
                        {
                            "year": 2023,
                            "value": 200,
                            "country": "DE",
                            "mint": "A",
                            "description": "Updated description",
                            "collectionId": "collection-1"
                        }
                        """), eq(CoinActionRequest.class))
                    ).thenReturn(request);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_UUID);
                    doNothing().when(coinService).save(any());
                    when(mapper.writeValueAsString(any())).thenThrow(new JacksonException("fail"){});
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "PATCH: exception during mapping of response"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(2002);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("not existing mint");
                    when(mapper.readValue("""
                        {
                            "year": 2022,
                            "value": 100,
                            "country": "DE",
                            "mint": "not existing mint",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "PATCH: error in mint translation because mint is not existing (IllegalArgumentException)"
            ),
            new CoinHandleTestcase(
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
                (coinService, collectionService, groupService, mapper) -> {
                    var request = mock(CoinActionRequest.class);
                    when(request.year()).thenReturn(0);
                    when(request.value()).thenReturn(100);
                    when(request.country()).thenReturn("DE");
                    when(request.collectionId()).thenReturn("collection-1");
                    when(request.description()).thenReturn("description");
                    when(request.mint()).thenReturn("A");
                    when(mapper.readValue("""
                        {
                            "year": 0,
                            "value": 100,
                            "country": "DE",
                            "mint": "A",
                            "description": "Testmünze",
                            "collectionId": "collection-1"
                        }
                        """, CoinActionRequest.class)
                    ).thenReturn(request);

                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                }, 
                500, 
                "{\"error\":\"Internal server error\"}",
                "PATCH: error in coin builder because year is to small (IllegalStateException)"
            )
        );
    }

    private static Stream<CoinHandleTestcase> coinDeleteTestcases(){
        return Stream.of(
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doNothing().when(coinService).delete(VALID_UUID);
                },
                204,
                null,
                "DELETE: happy path"
            ),
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    when(coinService.getById(VALID_UUID)).thenThrow(new EuroCoinNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: coin to delete not found"
            ),
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: collection for coin not found"
            ),
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupNotFoundException("not found"));
                },
                404,
                "{\"error\":\"Resource not found\"}",
                "DELETE: group of collection of coin not found"
            ),
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: exception retriving collection for coin (EuroCoinCollectionGetByIdException)"
            ),
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    when(collectionService.getById("collection-1")).thenThrow(new EuroCoinCollectionCoinsLoadException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: exception retriving collection for coin (EuroCoinCollectionCoinsLoadException)"
            ),
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn("other user");
                    when(groupService.getById("group-1")).thenThrow(new EuroCoinCollectionGroupGetByIdException("fail"));
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: exception retriving group of collection of coin"
            ),
            new CoinHandleTestcase(
                "DELETE",
                PREFIX + "/" + VALID_UUID,
                null,
                USER_ID,
                (coinService, collectionService, groupService, mapper) -> {
                    var mockCoin = createMockCoin(
                        VALID_UUID,
                        2002,
                        CoinValue.ONE_EURO,
                        CoinCountry.GERMANY,
                        Mint.BERLIN,
                        "collection-1",
                        "description"
                    );
                    when(coinService.getById(VALID_UUID)).thenReturn(mockCoin);
                    var mockCollection = mock(EuroCoinCollection.class);
                    when(mockCollection.getGroupId()).thenReturn("group-1");
                    when(collectionService.getById("collection-1")).thenReturn(mockCollection);
                    var mockGroup = mock(EuroCoinCollectionGroup.class);
                    when(mockGroup.getOwnerId()).thenReturn(USER_ID);
                    when(groupService.getById("group-1")).thenReturn(mockGroup);
                    doThrow(new EuroCoinDeleteException("fail")).when(coinService).delete(VALID_UUID);
                },
                500,
                "{\"error\":\"Internal server error\"}",
                "DELETE: deleting coin fails"
            )
        );
    }
    
    private static Stream<CoinHandleTestcase> coinHandleTestcases() {
        return Stream.concat(
            Stream.of(
                new CoinHandleTestcase(
                    "PUT",
                    PREFIX,
                    null,
                    USER_ID,
                    (coinService, collectionService, groupService, mapper) -> {},
                    405,
                    null,
                    "Should trigger default case in handle-switch (method not allowed)"
                )
            ),
            Stream.of(
                coinGetTestcases(),
                coinCreateTestcases(),
                coinUpdateTestcases(),
                coinDeleteTestcases()
            ).flatMap(s -> s)
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("coinHandleTestcases")
    void testHandle(CoinHandleTestcase testcase) throws IOException {
        var collectionService = mock(EuroCoinCollectionStorageService.class);
        var groupService = mock(EuroCoinCollectionGroupStorageService.class);
        var coinService = mock(EuroCoinStorageService.class);
        var mapper = mock(ObjectMapper.class);
        CoinHandler handler = new CoinHandler(coinService, collectionService, groupService, mapper);
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
            testcase.mockSetup().setup(coinService, collectionService, groupService, mapper);
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
