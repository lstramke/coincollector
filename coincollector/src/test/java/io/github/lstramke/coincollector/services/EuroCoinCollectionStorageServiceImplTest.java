package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionCoinsLoadException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionUpdateException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinUpdateException;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionStorageRepository;

public class EuroCoinCollectionStorageServiceImplTest {

    private record CoinData(CoinValue value, CoinCountry country, int year) {}

    private final static EuroCoinCollection dummyCollectionOnlyMetadata = new EuroCoinCollection("only metadata collection", "group2");
    private final static List<EuroCoin> coinsForMetadataCollection = Stream.of(
        new CoinData(CoinValue.FIFTY_CENTS, CoinCountry.NETHERLANDS, 2020),
        new CoinData(CoinValue.TWO_CENTS, CoinCountry.SPAIN, 2019),
        new CoinData(CoinValue.TWO_EUROS, CoinCountry.AUSTRIA, 2022)
    )
    .map(
        data -> new EuroCoinBuilder()
            .setYear(data.year)
            .setValue(data.value)
            .setMintCountry(data.country)
            .setCollectionId(dummyCollectionOnlyMetadata.getId())
            .build()
    )
    .toList();

    private final static EuroCoinCollection dummyCollection = new EuroCoinCollection("dummy collection", "group1");
    static {
        Stream.of(
            new CoinData(CoinValue.FIFTY_CENTS, CoinCountry.BELGIUM, 2020),
            new CoinData(CoinValue.TWO_CENTS, CoinCountry.FRANCE, 2021),
            new CoinData(CoinValue.TWO_EUROS, CoinCountry.ITALY, 2022)
        )
        .map(data -> new EuroCoinBuilder()
            .setYear(data.year)
            .setValue(data.value)
            .setMintCountry(data.country)
            .setCollectionId(dummyCollection.getId())
            .build())
        .forEach(dummyCollection::addCoin);
    }

    private record SaveInternalConnectionTestcase(
        EuroCoinCollection collection,
        boolean getConnectionThrows,
        boolean repositoryCreateThrows,
        boolean coinServiceSaveThrows,
        boolean simulateCoinAlreadyExists,
        boolean coinServiceUpdateThrows,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<SaveInternalConnectionTestcase> saveInternalConnectionTestcases(){
        return Stream.of(
            new SaveInternalConnectionTestcase(dummyCollection, false, false, false, false, false, null, "save is successful"),
            new SaveInternalConnectionTestcase(dummyCollection, true, false, false, false, false, EuroCoinCollectionSaveException.class, "getConnection throws"),
            new SaveInternalConnectionTestcase(dummyCollection, false, true, false, false, false, EuroCoinCollectionSaveException.class, "collection create throws"),
            new SaveInternalConnectionTestcase(dummyCollection, false, false, true, false, false, EuroCoinCollectionSaveException.class, "coin save throws"),
            new SaveInternalConnectionTestcase(dummyCollection, false, false, false, true, false, null, "coin already exists"),
            new SaveInternalConnectionTestcase(dummyCollection, false, false, true, true, true, EuroCoinCollectionSaveException.class, "coin update throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("saveInternalConnectionTestcases")
    void testSaveInternalConnection(SaveInternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryCreateThrows){
                    doThrow(new SQLException()).when(repository).create(connection, testcase.collection);
                } else {
                    doNothing().when(repository).create(connection, testcase.collection);

                    if(testcase.coinServiceSaveThrows){
                        doThrow(new EuroCoinSaveException("coinId")).when(coinStorageService).save(any(EuroCoin.class), eq(connection));
                    } else {
                        if (testcase.simulateCoinAlreadyExists) {
                            doThrow(new EuroCoinAlreadyExistsException("coinId")).when(coinStorageService).save(any(EuroCoin.class), eq(connection));
                            if(testcase.coinServiceUpdateThrows){
                                doThrow(new EuroCoinUpdateException("coinId")).when(coinStorageService).update(any(EuroCoin.class), eq(connection));
                            } else {
                                doNothing().when(coinStorageService).update(any(EuroCoin.class), eq(connection));
                            }
                        }
                    }
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.save(testcase.collection));
                if(!testcase.getConnectionThrows){
                    verify(connection).setAutoCommit(false);
                    verify(connection).rollback();
                }
                verify(connection, never()).commit();
            } else {
                assertDoesNotThrow(() -> service.save(testcase.collection));
                verify(dataSource).getConnection();
                verify(connection).setAutoCommit(false);
                verify(connection).commit();
                verify(connection, never()).rollback();
                verify(repository).create(connection, testcase.collection);
                verify(coinStorageService, atLeastOnce()).save(any(EuroCoin.class), eq(connection));
                if (testcase.simulateCoinAlreadyExists) {
                    verify(coinStorageService, atLeastOnce()).update(any(EuroCoin.class), eq(connection));
                }
            }   
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record SaveExternalConnectionTestcase(
        EuroCoinCollection collection,
        boolean repositoryCreateThrows,
        boolean coinServiceSaveThrows,
        boolean simulateCoinAlreadyExists,
        boolean coinServiceUpdateThrows,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<SaveExternalConnectionTestcase> saveExternalConnectionTestcases(){
        return Stream.of(
            new SaveExternalConnectionTestcase(dummyCollection, false, false, false, false, null, "save is successful"),
            new SaveExternalConnectionTestcase(dummyCollection, true, false, false, false, EuroCoinCollectionSaveException.class, "collection create throws"),
            new SaveExternalConnectionTestcase(dummyCollection, false, true, false, false, EuroCoinCollectionSaveException.class, "coin save throws"),
            new SaveExternalConnectionTestcase(dummyCollection, false, false, true, false, null, "coin already exists"),
            new SaveExternalConnectionTestcase(dummyCollection, false, true, true, true, EuroCoinCollectionSaveException.class, "coin update throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("saveExternalConnectionTestcases")
    void testSaveExternalConnection(SaveExternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.repositoryCreateThrows){
                doThrow(new SQLException()).when(repository).create(connection, testcase.collection);
            } else {
                doNothing().when(repository).create(connection, testcase.collection);
                if(testcase.coinServiceSaveThrows){
                    doThrow(new EuroCoinSaveException("coinId")).when(coinStorageService).save(any(EuroCoin.class), eq(connection));
                } else {
                    if (testcase.simulateCoinAlreadyExists) {
                        doThrow(new EuroCoinAlreadyExistsException("coinId")).when(coinStorageService).save(any(EuroCoin.class), eq(connection));
                        if(testcase.coinServiceUpdateThrows){
                            doThrow(new EuroCoinUpdateException("coinId")).when(coinStorageService).update(any(EuroCoin.class), eq(connection));
                        } else {
                            doNothing().when(coinStorageService).update(any(EuroCoin.class), eq(connection));
                        }
                    }
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.save(testcase.collection, connection));
            } else {
                assertDoesNotThrow(() -> service.save(testcase.collection, connection));
                verify(repository).create(connection, testcase.collection);
                verify(coinStorageService, atLeastOnce()).save(any(EuroCoin.class), eq(connection));
                if (testcase.simulateCoinAlreadyExists) {
                    verify(coinStorageService, atLeastOnce()).update(any(EuroCoin.class), eq(connection));
                }
            }   
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetByIdInternalConnectionTestcase(
        String collectionId,
        Optional<EuroCoinCollection> readReturn,
        boolean getConnectionThrows,
        boolean repositoryReadThrows,
        boolean coinServiceGetAllThrows,
        Class<? extends Exception> expectedException,
        EuroCoinCollection expectedCollection,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetByIdInternalConnectionTestcase> getByIdInternalConnectionTestcases(){
        return Stream.of(
            new GetByIdInternalConnectionTestcase(dummyCollectionOnlyMetadata.getId(), Optional.of(dummyCollectionOnlyMetadata), false, false, false, null, dummyCollectionOnlyMetadata, "getById is successful"),
            new GetByIdInternalConnectionTestcase(dummyCollectionOnlyMetadata.getId(), Optional.of(dummyCollectionOnlyMetadata), true, false, false, EuroCoinCollectionGetByIdException.class, null, "getConnection throws"),
            new GetByIdInternalConnectionTestcase(dummyCollectionOnlyMetadata.getId(), Optional.of(dummyCollectionOnlyMetadata), false, true, false, EuroCoinCollectionGetByIdException.class, null, "collection read throws"),
            new GetByIdInternalConnectionTestcase(dummyCollectionOnlyMetadata.getId(), Optional.of(dummyCollectionOnlyMetadata), false, false, true, EuroCoinCollectionCoinsLoadException.class, null, "coin service getAll throws"),
            new GetByIdInternalConnectionTestcase("notExistingId", Optional.empty(), false, false, false, EuroCoinCollectionNotFoundException.class, null, "coin collection doesn't exists")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getByIdInternalConnectionTestcases")
    void testGetByIdInternalConnection(GetByIdInternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryReadThrows){
                    doThrow(new SQLException()).when(repository).read(connection, testcase.collectionId);
                } else {
                    doReturn(testcase.readReturn).when(repository).read(connection, testcase.collectionId);

                    if(testcase.coinServiceGetAllThrows){
                        doThrow(new EuroCoinGetAllException()).when(coinStorageService).getAll(connection);
                    } else {
                        doReturn(coinsForMetadataCollection).when(coinStorageService).getAll(connection);
                    }
                }
            }
            
            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getById(testcase.collectionId));
            } else {
                EuroCoinCollection result = service.getById(testcase.collectionId);
                assertTrue(testcase.expectedCollection.getId().equals(result.getId()));
                assertTrue(testcase.expectedCollection.getName().equals(result.getName()));
                assertTrue(testcase.expectedCollection.getGroupId().equals(result.getGroupId()));
                assertTrue(coinsForMetadataCollection.equals(result.getCoins()));
                verify(dataSource).getConnection();
                verify(repository).read(connection, testcase.collectionId);
                verify(coinStorageService).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }

    }

    private record GetByIdExternalConnectionTestcase(
        String collectionId,
        Optional<EuroCoinCollection> readReturn,
        boolean repositoryReadThrows,
        boolean coinServiceGetAllThrows,
        Class<? extends Exception> expectedException,
        EuroCoinCollection expectedCollection,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetByIdExternalConnectionTestcase> getByIdExternalConnectionTestcases(){
        return Stream.of(
            new GetByIdExternalConnectionTestcase(dummyCollectionOnlyMetadata.getId(), Optional.of(dummyCollectionOnlyMetadata), false, false, null, dummyCollectionOnlyMetadata, "getById is successful"),
            new GetByIdExternalConnectionTestcase(dummyCollectionOnlyMetadata.getId(), Optional.of(dummyCollectionOnlyMetadata), true, false, EuroCoinCollectionGetByIdException.class, null, "collection read throws"),
            new GetByIdExternalConnectionTestcase(dummyCollectionOnlyMetadata.getId(), Optional.of(dummyCollectionOnlyMetadata), false, true, EuroCoinCollectionCoinsLoadException.class, null, "coin service getAll throws"),
            new GetByIdExternalConnectionTestcase("notExistingId", Optional.empty(), false, false, EuroCoinCollectionNotFoundException.class, null, "coin collection doesn't exists")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getByIdExternalConnectionTestcases")
    void testGetByIdExternalConnection(GetByIdExternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.repositoryReadThrows){
                doThrow(new SQLException()).when(repository).read(connection, testcase.collectionId);
            } else {
                doReturn(testcase.readReturn).when(repository).read(connection, testcase.collectionId);

                if(testcase.coinServiceGetAllThrows){
                    doThrow(new EuroCoinGetAllException()).when(coinStorageService).getAll(connection);
                } else {
                    doReturn(coinsForMetadataCollection).when(coinStorageService).getAll(connection);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getById(testcase.collectionId, connection));
            } else {
                EuroCoinCollection result = service.getById(testcase.collectionId, connection);
                assertEquals(testcase.expectedCollection, result);
                verify(repository).read(connection, testcase.collectionId);
                verify(coinStorageService).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());      
        }
    }
    
    private record UpdateInternalConnectionTestcase(
        EuroCoinCollection collection,
        boolean getConnectionThrows,
        boolean repositoryUpdateThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<UpdateInternalConnectionTestcase> updateInternalConnectionTestcases(){
        return Stream.of(
            new UpdateInternalConnectionTestcase(dummyCollection, false, false, null, "update is successful"),
            new UpdateInternalConnectionTestcase(dummyCollection, true, false, EuroCoinCollectionUpdateException.class, "getConnection throws"),
            new UpdateInternalConnectionTestcase(dummyCollection, false, true, EuroCoinCollectionUpdateException.class, "repository update throws")
        );
    }
    
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateInternalConnectionTestcases")
    void testUpdateInternalConnection(UpdateInternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryUpdateThrows){
                    doThrow(new SQLException()).when(repository).update(connection, testcase.collection);
                } else {
                    doNothing().when(repository).update(connection, testcase.collection);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.updateMetadata(testcase.collection));
            } else {
                assertDoesNotThrow(() -> service.updateMetadata(testcase.collection));
                verify(dataSource).getConnection();
                verify(repository).update(connection, testcase.collection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateExternalConnectionTestcase(
        EuroCoinCollection collection,
        boolean repositoryUpdateThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<UpdateExternalConnectionTestcase> updateExternalConnectionTestcases(){
        return Stream.of(
            new UpdateExternalConnectionTestcase(dummyCollection, false, null, "update is successful"),
            new UpdateExternalConnectionTestcase(dummyCollection, true, EuroCoinCollectionUpdateException.class, "repository update throws")
        );
    }
    
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateExternalConnectionTestcases")
    void testUpdateExternalConnection(UpdateExternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.repositoryUpdateThrows){
                doThrow(new SQLException()).when(repository).update(connection, testcase.collection);
            } else {
                doNothing().when(repository).update(connection, testcase.collection);
            }
            
            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.updateMetadata(testcase.collection, connection));
            } else {
                assertDoesNotThrow(() -> service.updateMetadata(testcase.collection, connection));
                verify(repository).update(connection, testcase.collection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteInternalConnectionTestcase(
        String collectionId,
        boolean getConnectionThrows,
        boolean repositoryDeleteThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<DeleteInternalConnectionTestcase> deleteInternalConnectionTestcases(){
        return Stream.of(
            new DeleteInternalConnectionTestcase("validId", false, false, null, "delete is successful"),
            new DeleteInternalConnectionTestcase("validId", true, false, EuroCoinCollectionDeleteException.class, "getConnection throws"),
            new DeleteInternalConnectionTestcase("validId", false, true, EuroCoinCollectionDeleteException.class, "repository delete throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteInternalConnectionTestcases")
    void testDeleteInternalConnection(DeleteInternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryDeleteThrows){
                    doThrow(new SQLException()).when(repository).delete(connection, testcase.collectionId);
                } else {
                    doNothing().when(repository).delete(connection, testcase.collectionId);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.delete(testcase.collectionId));
            } else {
                assertDoesNotThrow(() -> service.delete(testcase.collectionId));
                verify(dataSource).getConnection();
                verify(repository).delete(connection, testcase.collectionId);
            }        
        } catch (SQLException e) {
             fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteExternalConnectionTestcase(
        String collectionId,
        boolean repositoryDeleteThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<DeleteExternalConnectionTestcase> deleteExternalConnectionTestcases(){
        return Stream.of(
            new DeleteExternalConnectionTestcase("validId", false, null, "delete is successful"),
            new DeleteExternalConnectionTestcase("validId", true, EuroCoinCollectionDeleteException.class, "repository delete throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteExternalConnectionTestcases")
    void testDeleteExternalConnection(DeleteExternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.repositoryDeleteThrows){
                doThrow(new SQLException()).when(repository).delete(connection, testcase.collectionId);
            } else {
                doNothing().when(repository).delete(connection, testcase.collectionId);
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.delete(testcase.collectionId, connection));
            } else {
                assertDoesNotThrow(() -> service.delete(testcase.collectionId, connection));
                verify(repository).delete(connection, testcase.collectionId);
            }
        } catch (SQLException e) {
             fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetAllInternalConnectionTestcase(
        boolean getConnectionThrows,
        boolean repositoryGetAllCollectionsThrows,
        boolean repositoryGetAllCoinsThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllInternalConnectionTestcase> getAllInternalConnectionTestcases(){
        return Stream.of(
            new GetAllInternalConnectionTestcase(false, false, false, null, "getAll is successful"),
            new GetAllInternalConnectionTestcase(true, false, false, EuroCoinCollectionGetAllException.class, "getConnection throws"),
            new GetAllInternalConnectionTestcase(false, true, false, EuroCoinCollectionGetAllException.class, "repository getAll collections throws"),
            new GetAllInternalConnectionTestcase(false, false, true, EuroCoinCollectionGetAllException.class, "repository getAll coins throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllInternalConnectionTestcases")
    void testGetAllInternalConnection(GetAllInternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryGetAllCollectionsThrows){
                    doThrow(new SQLException()).when(repository).getAll(connection);
                } else {
                    doReturn(List.of(dummyCollection, dummyCollectionOnlyMetadata)).when(repository).getAll(connection);

                    if(testcase.repositoryGetAllCoinsThrows){
                        doThrow(new EuroCoinGetAllException()).when(coinStorageService).getAll(connection);
                    } else {
                        List<EuroCoin> allCoins = Stream.concat(dummyCollection.getCoins().stream(), coinsForMetadataCollection.stream()).toList();
                        doReturn(allCoins).when(coinStorageService).getAll(connection);
                    }
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getAll());
            } else {
                List<EuroCoinCollection> result = service.getAll();
                assertTrue(result.stream().anyMatch(c -> c.getId().equals(dummyCollection.getId())));
                assertTrue(result.stream().anyMatch(c -> c.getId().equals(dummyCollectionOnlyMetadata.getId())));
                assertTrue(result.stream().filter(c -> c.getId().equals(dummyCollectionOnlyMetadata.getId())).allMatch(c -> c.getCoins().stream().allMatch(d -> d.getCollectionId().equals(dummyCollectionOnlyMetadata.getId()))));
                verify(dataSource).getConnection();
                verify(repository).getAll(connection);
                verify(coinStorageService).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetAllExternalConnectionTestcase(
        boolean repositoryGetAllCollectionsThrows,
        boolean repositoryGetAllCoinsThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllExternalConnectionTestcase> getAllExternalConnectionTestcases(){
        return Stream.of(
            new GetAllExternalConnectionTestcase(false, false, null, "getAll is successful"),
            new GetAllExternalConnectionTestcase(true, false, EuroCoinCollectionGetAllException.class, "repository getAll collections throws"),
            new GetAllExternalConnectionTestcase(false, true, EuroCoinCollectionGetAllException.class, "repository getAll coins throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllExternalConnectionTestcases")
    void testGetAllExternalConnection(GetAllExternalConnectionTestcase testcase){
        EuroCoinCollectionStorageRepository repository = mock(EuroCoinCollectionStorageRepository.class);
        EuroCoinStorageService coinStorageService = mock(EuroCoinStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionStorageService service = new EuroCoinCollectionStorageServiceImpl(repository, dataSource, coinStorageService);

        try {

            if(testcase.repositoryGetAllCollectionsThrows){
                doThrow(new SQLException()).when(repository).getAll(connection);
            } else {
                doReturn(List.of(dummyCollection, dummyCollectionOnlyMetadata)).when(repository).getAll(connection);
                if(testcase.repositoryGetAllCoinsThrows){
                    doThrow(new EuroCoinGetAllException()).when(coinStorageService).getAll(connection);
                } else {
                    List<EuroCoin> allCoins = Stream.concat(dummyCollection.getCoins().stream(), coinsForMetadataCollection.stream()).toList();
                    doReturn(allCoins).when(coinStorageService).getAll(connection);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getAll(connection));
            } else {
                List<EuroCoinCollection> result = service.getAll(connection);
                assertTrue(result.stream().anyMatch(c -> c.getId().equals(dummyCollection.getId())));
                assertTrue(result.stream().filter(c -> c.getId().equals(dummyCollectionOnlyMetadata.getId())).allMatch(c -> c.getCoins().stream().allMatch(d -> d.getCollectionId().equals(dummyCollectionOnlyMetadata.getId()))));
                verify(repository).getAll(connection);
                verify(coinStorageService).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

}
