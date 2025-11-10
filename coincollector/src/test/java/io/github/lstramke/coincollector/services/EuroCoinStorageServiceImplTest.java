package io.github.lstramke.coincollector.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinAlreadyExistsException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinDeleteException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinNotFoundException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinSaveException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinUpdateException;
import io.github.lstramke.coincollector.exceptions.euroCoinExceptions.EuroCoinGetAllException;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoin;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.Mint;
import io.github.lstramke.coincollector.repositories.EuroCoinStorageRepository;

public class EuroCoinStorageServiceImplTest {

    private static final EuroCoin dummyCoin = new EuroCoinBuilder()
                                                .setYear(2002)
                                                .setValue(CoinValue.ONE_EURO)
                                                .setMintCountry(CoinCountry.GERMANY)
                                                .setMint(Mint.BERLIN)
                                                .setCollectionId("COL-1")
                                                .build();;

    private record SaveInternalConnectionTestcase(
        EuroCoin coin,
        boolean existsResult,
        boolean getConnectionThrows,
        boolean repositoryCreateThrows,
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
            new SaveInternalConnectionTestcase(null, false, false, false, IllegalArgumentException.class, "Null coin"),
            new SaveInternalConnectionTestcase(dummyCoin, true, false, false, EuroCoinAlreadyExistsException.class, "coin already exists"),
            new SaveInternalConnectionTestcase(dummyCoin, false, false, false, null, "coin-save is successful"),
            new SaveInternalConnectionTestcase(dummyCoin, false, true, false, EuroCoinSaveException.class, "get connection throws"),
            new SaveInternalConnectionTestcase(dummyCoin, false, false, true, EuroCoinSaveException.class, "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("saveInternalConnectionTestcases")
    void testSaveInternalConnection(SaveInternalConnectionTestcase testcase) {
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.coin != null){
                if(testcase.getConnectionThrows){
                    when(dataSource.getConnection()).thenThrow(new SQLException());
                } else {
                    when(dataSource.getConnection()).thenReturn(connection);
                    when(repository.exists(connection, testcase.coin.getId())).thenReturn(testcase.existsResult);
                    if(testcase.repositoryCreateThrows){
                        doThrow(new SQLException("Repo create error")).when(repository).create(connection, testcase.coin);
                    } else {
                        doNothing().when(repository).create(connection, testcase.coin);
                    }
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.save(testcase.coin));
            } else {
                assertDoesNotThrow(() -> service.save(testcase.coin));
                verify(dataSource).getConnection();
                verify(repository).exists(connection, testcase.coin.getId());
                verify(repository).create(connection, testcase.coin);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record SaveExternalConnectionTestcase(
        EuroCoin coin,
        boolean existsResult,
        boolean repositoryCreateThrows,
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
            new SaveExternalConnectionTestcase(null, false, false, IllegalArgumentException.class, "Null coin"),
            new SaveExternalConnectionTestcase(dummyCoin, true, false, EuroCoinAlreadyExistsException.class, "coin already exists"),
            new SaveExternalConnectionTestcase(dummyCoin, false, false, null, "coin-save is successful"),
            new SaveExternalConnectionTestcase(dummyCoin, false, true, EuroCoinSaveException.class, "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("saveExternalConnectionTestcases")
    void testSaveExternalConnection(SaveExternalConnectionTestcase testcase) {
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.coin != null){
                when(repository.exists(connection, testcase.coin.getId())).thenReturn(testcase.existsResult);
                if(testcase.repositoryCreateThrows){
                    doThrow(new SQLException("Repo create error")).when(repository).create(connection, testcase.coin);
                } else {
                    doNothing().when(repository).create(connection, testcase.coin);
                }
            }

            if (testcase.expectedException != null) {
                assertThrows(testcase.expectedException, () -> service.save(testcase.coin, connection));
            } else {
                assertDoesNotThrow(() -> service.save(testcase.coin, connection));
                verify(repository).exists(connection, testcase.coin.getId());
                verify(repository).create(connection, testcase.coin);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetByIdInternalConnectionTestcase(
        String coinId,
        boolean getConnectionThrows,
        boolean repositoryReadThrows,
        Optional<EuroCoin> readReturn,
        EuroCoin expectedCoin,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetByIdInternalConnectionTestcase> getByIdInternalConnectionTestcases(){
        return Stream.of(
            new GetByIdInternalConnectionTestcase("validId", false, false, Optional.of(dummyCoin), dummyCoin, null, "getById is successful"),
            new GetByIdInternalConnectionTestcase("validId", true, false, null, null, EuroCoinNotFoundException.class, "getConnection throws"),
            new GetByIdInternalConnectionTestcase("validId", false, true, null, null, EuroCoinNotFoundException.class, "repository throws"),
            new GetByIdInternalConnectionTestcase("validId", false, false, Optional.empty(), null, EuroCoinNotFoundException.class, "repository returns Optional.Empty")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getByIdInternalConnectionTestcases")
    void testGetByIdInternalConnection(GetByIdInternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryReadThrows){
                    when(repository.read(connection, testcase.coinId)).thenThrow(new SQLException());
                } else {
                    when(repository.read(connection, testcase.coinId)).thenReturn(testcase.readReturn);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getById(testcase.coinId));
            } else {
                EuroCoin result = service.getById(testcase.coinId);
                assertEquals(testcase.expectedCoin, result);
                verify(dataSource).getConnection();
                verify(repository).read(connection, testcase.coinId);
            }
            
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetByIdExternalConnectionTestcase(
        String coinId,
        boolean repositoryReadThrows,
        Optional<EuroCoin> readReturn,
        EuroCoin expectedCoin,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetByIdExternalConnectionTestcase> getByIdExternalConnectionTestcases(){
        return Stream.of(
            new GetByIdExternalConnectionTestcase("validId", false, Optional.of(dummyCoin), dummyCoin, null, "getById is successful"),
            new GetByIdExternalConnectionTestcase("validId", true, null, null, EuroCoinNotFoundException.class, "repository throws"),
            new GetByIdExternalConnectionTestcase("validId", false, Optional.empty(), null, EuroCoinNotFoundException.class, "repository returns Optional.Empty")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getByIdExternalConnectionTestcases")
    void testGetByIdExternalConnection(GetByIdExternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.repositoryReadThrows){
                when(repository.read(connection, testcase.coinId)).thenThrow(new SQLException());
            } else {
                when(repository.read(connection, testcase.coinId)).thenReturn(testcase.readReturn);
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getById(testcase.coinId, connection));
            } else {
                EuroCoin result = service.getById(testcase.coinId, connection);
                assertEquals(testcase.expectedCoin, result);
                verify(repository).read(connection, testcase.coinId);
            }
            
        } catch (SQLException e) {
           fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateInternalConnectionTestcase(
        EuroCoin coin,
        boolean getConnectionThrows,
        boolean repositoryUpdateThrows,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<UpdateInternalConnectionTestcase> updateInternalConnectionTestcases(){
        return Stream.of(
            new UpdateInternalConnectionTestcase(dummyCoin, false, false, null, "update is successful"),
            new UpdateInternalConnectionTestcase(null, false, false, IllegalArgumentException.class, "null coin"),
            new UpdateInternalConnectionTestcase(dummyCoin, true, false, EuroCoinUpdateException.class, "getConnection throws"),
            new UpdateInternalConnectionTestcase(dummyCoin, false, true, EuroCoinUpdateException.class, "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateInternalConnectionTestcases")
    void testUpdateInternalConnection(UpdateInternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryUpdateThrows){
                    doThrow(new SQLException()).when(repository).update(connection, testcase.coin);
                } else if(testcase.coin == null) {
                    doThrow(new IllegalArgumentException()).when(repository).update(connection, testcase.coin);
                } else {
                    doNothing().when(repository).update(connection, testcase.coin);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.update(testcase.coin));
            } else {
                assertDoesNotThrow(() -> service.update(testcase.coin));
                verify(dataSource).getConnection();
                verify(repository).update(connection, testcase.coin);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record UpdateExternalConnectionTestcase(
        EuroCoin coin,
        boolean repositoryUpdateThrows,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<UpdateExternalConnectionTestcase> updateExternalConnectionTestcases(){
        return Stream.of(
            new UpdateExternalConnectionTestcase(dummyCoin, false, null, "update is successful"),
            new UpdateExternalConnectionTestcase(null, false, IllegalArgumentException.class, "null coin"),
            new UpdateExternalConnectionTestcase(dummyCoin, true, EuroCoinUpdateException.class, "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("updateExternalConnectionTestcases")
    void testUpdateExternalConnection(UpdateExternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.repositoryUpdateThrows){
                doThrow(new SQLException()).when(repository).update(connection, testcase.coin);
            } else if(testcase.coin == null) {
                doThrow(new IllegalArgumentException()).when(repository).update(connection, testcase.coin);
            } else {
                doNothing().when(repository).update(connection, testcase.coin);
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.update(testcase.coin, connection));
            } else {
                assertDoesNotThrow(() -> service.update(testcase.coin, connection));
                verify(repository).update(connection, testcase.coin);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteInternalConnectionTestcase(
        String coinId,
        boolean getConnectionThrows,
        boolean repositoryDeleteThrows,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<DeleteInternalConnectionTestcase> deleteInternalConnectionTestcases(){
        return Stream.of(
            new DeleteInternalConnectionTestcase("validId", false, false, null, "update is successful"),
            new DeleteInternalConnectionTestcase(null, false, false, IllegalArgumentException.class, "null coin"),
            new DeleteInternalConnectionTestcase("validId", true, false, EuroCoinDeleteException.class, "getConnection throws"),
            new DeleteInternalConnectionTestcase("validId", false, true, EuroCoinDeleteException.class, "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteInternalConnectionTestcases")
    void testDeleteInternalConnection(DeleteInternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryDeleteThrows){
                    doThrow(new SQLException()).when(repository).delete(connection, testcase.coinId);
                } else if(testcase.coinId == null) {
                    doThrow(new IllegalArgumentException()).when(repository).delete(connection, testcase.coinId);
                } else {
                    doNothing().when(repository).delete(connection, testcase.coinId);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.delete(testcase.coinId));
            } else {
                assertDoesNotThrow(() -> service.delete(testcase.coinId));
                verify(dataSource).getConnection();
                verify(repository).delete(connection, testcase.coinId);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record DeleteExternalConnectionTestcase(
        String coinId,
        boolean repositoryDeleteThrows,
        Class<? extends Exception> expectedException,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<DeleteExternalConnectionTestcase> deleteExternalConnectionTestcases(){
        return Stream.of(
            new DeleteExternalConnectionTestcase("validId", false, null, "update is successful"),
            new DeleteExternalConnectionTestcase(null, false, IllegalArgumentException.class, "null coin"),
            new DeleteExternalConnectionTestcase("validId", true, EuroCoinDeleteException.class, "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("deleteExternalConnectionTestcases")
    void testDeleteExternalConnection(DeleteExternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.repositoryDeleteThrows){
                doThrow(new SQLException()).when(repository).delete(connection, testcase.coinId);
            } else if(testcase.coinId == null) {
                doThrow(new IllegalArgumentException()).when(repository).delete(connection, testcase.coinId);
            } else {
                doNothing().when(repository).delete(connection, testcase.coinId);
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.delete(testcase.coinId, connection));
            } else {
                assertDoesNotThrow(() -> service.delete(testcase.coinId, connection));
                verify(repository).delete(connection, testcase.coinId);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetAllInternalConnectionTestcase(
        boolean getConnectionThrows,
        boolean repositoryDeleteThrows,
        Class<? extends Exception> expectedException,
        List<EuroCoin> expectedCoins,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllInternalConnectionTestcase> getAllInternalConnectionTestcases(){
        return Stream.of(
            new GetAllInternalConnectionTestcase(false, false, null, List.of(dummyCoin), "update is successful"),
            new GetAllInternalConnectionTestcase(true, false, EuroCoinGetAllException.class, List.of(), "getConnection throws"),
            new GetAllInternalConnectionTestcase(false, true, EuroCoinGetAllException.class, List.of(), "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllInternalConnectionTestcases")
    void testGetAllInternalConnection(GetAllInternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.getConnectionThrows){
                when(dataSource.getConnection()).thenThrow(new SQLException());
            } else {
                when(dataSource.getConnection()).thenReturn(connection);

                if(testcase.repositoryDeleteThrows){
                    doThrow(new SQLException()).when(repository).getAll(connection);
                } else {
                    when(repository.getAll(connection)).thenReturn(testcase.expectedCoins);
                }
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getAll());
            } else {
                assertDoesNotThrow(() -> service.getAll());
                verify(dataSource).getConnection();
                verify(repository).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }

    private record GetAllExternalConnectionTestcase(
        boolean repositoryDeleteThrows,
        Class<? extends Exception> expectedException,
        List<EuroCoin> expectedCoins,
        String description
    ){
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<GetAllExternalConnectionTestcase> getAllExternalConnectionTestcases(){
        return Stream.of(
            new GetAllExternalConnectionTestcase(false, null, List.of(dummyCoin), "update is successful"),
            new GetAllExternalConnectionTestcase(true, EuroCoinGetAllException.class, List.of(), "repository throws")
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("getAllExternalConnectionTestcases")
    void testGetAllExternalConnection(GetAllExternalConnectionTestcase testcase){
        EuroCoinStorageRepository repository = mock(EuroCoinStorageRepository.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        EuroCoinStorageService service = new EuroCoinStorageServiceImpl(repository, dataSource);

        try {
            if(testcase.repositoryDeleteThrows){
                doThrow(new SQLException()).when(repository).getAll(connection);
            } else {
                when(repository.getAll(connection)).thenReturn(testcase.expectedCoins);
            }

            if(testcase.expectedException != null){
                assertThrows(testcase.expectedException, () -> service.getAll(connection));
            } else {
                assertDoesNotThrow(() -> service.getAll(connection));
                verify(repository).getAll(connection);
            }
        } catch (SQLException e) {
            fail("SQLException should not occur with mocks: " + e.getMessage());
        }
    }
}
