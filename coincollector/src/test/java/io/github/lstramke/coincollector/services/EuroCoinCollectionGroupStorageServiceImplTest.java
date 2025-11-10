package io.github.lstramke.coincollector.services;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionException.EuroCoinCollectionUpdateException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetByIdException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupUpdateException;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.repositories.EuroCoinCollectionGroupStorageRepository;

public class EuroCoinCollectionGroupStorageServiceImplTest {
    private record CoinData(CoinValue value, CoinCountry country, int year) {}

    private static final EuroCoinCollectionGroup dummyGroup = new EuroCoinCollectionGroup("dummyGroup", "testOwner");
    private static final EuroCoinCollectionGroup dummyGroup2 = new EuroCoinCollectionGroup("dummyGroup2", "owner2");
    private final static List<EuroCoinCollection> dummyCollections = new ArrayList<>();
    private final static EuroCoinCollection dummyCollection = new EuroCoinCollection("dummy collection", dummyGroup.getId());
    private final static EuroCoinCollection dummyCollection2 = new EuroCoinCollection("dummy collection", dummyGroup2.getId());
    static {
        Stream.of(
            new CoinData(CoinValue.FIFTY_CENTS, CoinCountry.BELGIUM, 2020),
            new CoinData(CoinValue.TWO_CENTS, CoinCountry.FRANCE, 2021),
            new CoinData(CoinValue.TWO_EUROS, CoinCountry.ITALY, 2022)
        )
        .map(data -> new EuroCoinBuilder()
            .setValue(data.value)
            .setYear(data.year)
            .setMintCountry(data.country)
            .setCollectionId(dummyCollection.getId())
            .build())
        .forEach(dummyCollection::addCoin);

        Stream.of(
            new CoinData(CoinValue.ONE_EURO, CoinCountry.ESTONIA, 2018),
            new CoinData(CoinValue.TWO_CENTS, CoinCountry.FRANCE, 2021),
            new CoinData(CoinValue.TWENTY_CENTS, CoinCountry.ITALY, 2006)
        )
        .map(data -> new EuroCoinBuilder()
            .setYear(data.year)
            .setValue(data.value)
            .setMintCountry(data.country)
            .setCollectionId(dummyCollection2.getId())
            .build())
        .forEach(dummyCollection2::addCoin);

        dummyCollections.add(dummyCollection);
        dummyCollections.add(dummyCollection2);

        dummyGroup.addCollection(dummyCollection);
    }

    private record SaveTestcase(
        EuroCoinCollectionGroup group,
        boolean getConnectionThrows,
        boolean repositoryCreateThrows,
        boolean collectionServiceSaveThrows,
        boolean simulateCollectionAlreadyExists,
        boolean collectionServiceUpdateMetadataThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<SaveTestcase> saveTestcases(){        
        return Stream.of(
            new SaveTestcase(
                null,
                false,
                false,
                false,
                false,
                false,
                IllegalArgumentException.class,
                "group is null"
            ),
            new SaveTestcase(
                dummyGroup,
                false,
                false,
                false,
                false,
                false,
                null,
                "save is successful"
            ),
            new SaveTestcase(
                dummyGroup,
                true,
                false,
                false,
                false,
                false,
                EuroCoinCollectionGroupSaveException.class,
                "getConnection throws"
            ),
            new SaveTestcase(
                dummyGroup,
                false,
                true,
                false,
                false,
                false,
                EuroCoinCollectionGroupSaveException.class,
                "group create throws"
            ),
            new SaveTestcase(
                dummyGroup,
                false,
                false,
                true,
                false,
                false,
                EuroCoinCollectionGroupSaveException.class,
                "collection save throws"
            ),
            new SaveTestcase(
                dummyGroup,
                false,
                false,
                false,
                true,
                false,
                null,
                "collection already exists"
            ),
            new SaveTestcase(
                dummyGroup,
                false,
                false,
                false,
                true,
                true,
                EuroCoinCollectionGroupSaveException.class,
                "collection updateMetadata throws"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("saveTestcases")
    void testSave(SaveTestcase testcase){
        EuroCoinCollectionGroupStorageRepository repository = mock(EuroCoinCollectionGroupStorageRepository.class);
        EuroCoinCollectionStorageService coinCollectionStorageService = mock(EuroCoinCollectionStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionGroupStorageService service = new EuroCoinCollectionGroupStorageServiceImpl(dataSource, repository, coinCollectionStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if (testcase.repositoryCreateThrows) {
                    doThrow(new SQLException()).when(repository).create(connection, testcase.group);
                } else {
                    doNothing().when(repository).create(connection, testcase.group);

                    if(testcase.collectionServiceSaveThrows){
                        doThrow(new EuroCoinCollectionSaveException("id")).when(coinCollectionStorageService).save(any(EuroCoinCollection.class), eq(connection));
                    } else {
                        if(testcase.simulateCollectionAlreadyExists){
                            doThrow(new EuroCoinCollectionAlreadyExistsException("id")).when(coinCollectionStorageService).save(any(EuroCoinCollection.class), eq(connection));
                        if(testcase.collectionServiceUpdateMetadataThrows){
                                doThrow(new EuroCoinCollectionUpdateException("coinId")).when(coinCollectionStorageService).updateMetadata(any(EuroCoinCollection.class), eq(connection));
                            } else {
                                doNothing().when(coinCollectionStorageService).updateMetadata(any(EuroCoinCollection.class), eq(connection));
                            }
                        }
                    }
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.save(testcase.group));
                if(!testcase.getConnectionThrows && testcase.group != null){
                    verify(connection).setAutoCommit(false);
                    verify(connection).rollback();
                }
                verify(connection, never()).commit();
            } else {
                assertDoesNotThrow(() -> service.save(testcase.group));
                verify(dataSource).getConnection();
                verify(connection).setAutoCommit(false);
                verify(connection).commit();
                verify(connection, never()).rollback();
                verify(repository).create(connection, testcase.group);
                verify(coinCollectionStorageService, atLeastOnce()).save(any(EuroCoinCollection.class), eq(connection));
                if (testcase.simulateCollectionAlreadyExists) {
                    verify(coinCollectionStorageService, atLeastOnce()).updateMetadata(any(EuroCoinCollection.class), eq(connection));
                }
            }   
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetByIdTestcase(
        String groupId,
        Optional<EuroCoinCollectionGroup> readReturn,
        boolean getConnectionThrows,
        boolean repositoryReadThrows,
        boolean collectionServiceGetAllThrows,
        Class<? extends Exception> expectedException,
        EuroCoinCollectionGroup expectedCollectionGroup,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetByIdTestcase> getByIdTestcases(){
        return Stream.of(
            new GetByIdTestcase(
                dummyGroup2.getId(),
                Optional.of(dummyGroup2),
                false,
                false,
                false,
                null,
                dummyGroup2,
                "getById is successful"
            ),
            new GetByIdTestcase(
                dummyGroup.getId(),
                Optional.empty(),
                true,
                false,
                false,
                EuroCoinCollectionGroupGetByIdException.class,
                null,
                "getConnection throws"
            ),
            new GetByIdTestcase(
                dummyGroup.getId(),
                Optional.empty(),
                false,
                true,
                false,
                EuroCoinCollectionGroupGetByIdException.class,
                null,
                "repository read throws"
            ),
            new GetByIdTestcase(
                "notExistingGroupId",
                Optional.empty(),
                false,
                false,
                false,
                EuroCoinCollectionGroupNotFoundException.class,
                null,
                "group not found"
            ),
            new GetByIdTestcase(
                dummyGroup.getId(),
                Optional.of(dummyGroup),
                false,
                false,
                true,
                EuroCoinCollectionGroupGetByIdException.class,
                null,
                "collection service getAll throws"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getByIdTestcases")
    void testGetById(GetByIdTestcase testcase){
        EuroCoinCollectionGroupStorageRepository repository = mock(EuroCoinCollectionGroupStorageRepository.class);
        EuroCoinCollectionStorageService coinCollectionStorageService = mock(EuroCoinCollectionStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionGroupStorageService service = new EuroCoinCollectionGroupStorageServiceImpl(dataSource, repository, coinCollectionStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryReadThrows){
                    doThrow(new SQLException()).when(repository).read(connection, testcase.groupId);
                } else {
                    doReturn(testcase.readReturn).when(repository).read(connection, testcase.groupId);

                    if(testcase.collectionServiceGetAllThrows){
                        doThrow(new EuroCoinCollectionGetAllException()).when(coinCollectionStorageService).getAll(eq(connection));
                    } else {
                        doReturn(dummyCollections).when(coinCollectionStorageService).getAll(eq(connection));
                    }
                }
            }
            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getById(testcase.groupId));
            } else {
                var result = service.getById(testcase.groupId);
                assertTrue(testcase.expectedCollectionGroup.getId().equals(result.getId()));
                assertTrue(testcase.expectedCollectionGroup.getName().equals(result.getName()));
                assertTrue(testcase.expectedCollectionGroup.getId().equals(result.getId()));

                dummyGroup2.addCollection(dummyCollection2);
                assertTrue(result.getCollections().size() == dummyCollections.size());
                assertTrue(result.getCollections().stream().allMatch(c -> dummyCollections.stream().anyMatch(d -> d.getId().equals(c.getId()))));
                verify(dataSource).getConnection();
                verify(repository).read(connection, testcase.groupId);
                verify(coinCollectionStorageService).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateMetadataTestcase(
        EuroCoinCollectionGroup group,
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

    private static Stream<UpdateMetadataTestcase> updateMetadataTestcases(){
        return Stream.of(
            new UpdateMetadataTestcase(
                null,
                false,
                false,
                IllegalArgumentException.class,
                "group is null"
            ),
            new UpdateMetadataTestcase(
                dummyGroup,
                false,
                false,
                null,
                "updateMetadata is successful"
            ),
            new UpdateMetadataTestcase(
                dummyGroup,
                true,
                false,
                EuroCoinCollectionGroupUpdateException.class,
                "getConnection throws"
            ),
            new UpdateMetadataTestcase(
                dummyGroup,
                false,
                true,
                EuroCoinCollectionGroupUpdateException.class,
                "repository update throws"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateMetadataTestcases")
    void testUpdateMetadata(UpdateMetadataTestcase testcase){
        EuroCoinCollectionGroupStorageRepository repository = mock(EuroCoinCollectionGroupStorageRepository.class);
        EuroCoinCollectionStorageService coinCollectionStorageService = mock(EuroCoinCollectionStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionGroupStorageService service = new EuroCoinCollectionGroupStorageServiceImpl(dataSource, repository, coinCollectionStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryUpdateThrows){
                    doThrow(new SQLException()).when(repository).update(connection, testcase.group);
                } else {
                    doNothing().when(repository).update(connection, testcase.group);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.updateMetadata(testcase.group));
            } else {
                assertDoesNotThrow(() -> service.updateMetadata(testcase.group));
                verify(dataSource).getConnection();
                verify(repository).update(connection, testcase.group);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteTestcase(
        String groupId,
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

    private static Stream<DeleteTestcase> deleteTestcases(){
        return Stream.of(
            new DeleteTestcase(
                dummyGroup.getId(),
                false,
                false,
                null,
                "delete is successful"
            ),
            new DeleteTestcase(
                dummyGroup.getId(),
                true,
                false,
                EuroCoinCollectionGroupDeleteException.class,
                "getConnection throws"
            ),
            new DeleteTestcase(
                dummyGroup.getId(),
                false,
                true,
                EuroCoinCollectionGroupDeleteException.class,
                "repository delete throws"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteTestcases")
    void testDelete(DeleteTestcase testcase){
        EuroCoinCollectionGroupStorageRepository repository = mock(EuroCoinCollectionGroupStorageRepository.class);
        EuroCoinCollectionStorageService coinCollectionStorageService = mock(EuroCoinCollectionStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionGroupStorageService service = new EuroCoinCollectionGroupStorageServiceImpl(dataSource, repository, coinCollectionStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryDeleteThrows){
                    doThrow(new SQLException()).when(repository).delete(connection, testcase.groupId);
                } else {
                    doNothing().when(repository).delete(connection, testcase.groupId);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.delete(testcase.groupId));
            } else {
                assertDoesNotThrow(() -> service.delete(testcase.groupId));
                verify(dataSource).getConnection();
                verify(repository).delete(connection, testcase.groupId);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetAllByUserTestcase(
        String userId,
        List<EuroCoinCollectionGroup> groupsReturn,
        boolean getConnectionThrows,
        boolean repositoryGetAllByUserThrows,
        boolean collectionServiceGetAllThrows,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllByUserTestcase> getAllByUserTestcases(){
        return Stream.of(
            new GetAllByUserTestcase(
                "testOwner",
                List.of(dummyGroup, dummyGroup2),
                false,
                false,
                false,
                null,
                "getAllByUser is successful"
            ),
            new GetAllByUserTestcase(
                "testOwner",
                List.of(),
                true,
                false,
                false,
                EuroCoinCollectionGroupGetAllException.class,
                "getConnection throws"
            ),
            new GetAllByUserTestcase(
                "testOwner",
                List.of(),
                false,
                true,
                false,
                EuroCoinCollectionGroupGetAllException.class,
                "repository getAllByUser throws"
            ),
            new GetAllByUserTestcase(
                "testOwner",
                List.of(dummyGroup, dummyGroup2),
                false,
                false,
                true,
                EuroCoinCollectionGroupGetAllException.class,
                "collection service getAll throws"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllByUserTestcases")
    void testGetAllByUser(GetAllByUserTestcase testcase){
        EuroCoinCollectionGroupStorageRepository repository = mock(EuroCoinCollectionGroupStorageRepository.class);
        EuroCoinCollectionStorageService coinCollectionStorageService = mock(EuroCoinCollectionStorageService.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinCollectionGroupStorageService service = new EuroCoinCollectionGroupStorageServiceImpl(dataSource, repository, coinCollectionStorageService);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryGetAllByUserThrows){
                    doThrow(new SQLException()).when(repository).getAllByUser(connection, testcase.userId);
                } else {
                    doReturn(testcase.groupsReturn).when(repository).getAllByUser(connection, testcase.userId);

                    if(testcase.collectionServiceGetAllThrows){
                        doThrow(new EuroCoinCollectionGetAllException()).when(coinCollectionStorageService).getAll(eq(connection));
                    } else {
                        doReturn(dummyCollections).when(coinCollectionStorageService).getAll(eq(connection));
                    }
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getAllByUser(testcase.userId));
            } else {
                assertDoesNotThrow(() -> {
                    var result = service.getAllByUser(testcase.userId);
                    assertTrue(result.size() == testcase.groupsReturn.size());
                    assertTrue(result.stream().allMatch(g -> testcase.groupsReturn.stream().anyMatch(r -> r.getId().equals(g.getId()))));
                });
                verify(dataSource).getConnection();
                verify(repository).getAllByUser(connection, testcase.userId);
                verify(coinCollectionStorageService).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }
}