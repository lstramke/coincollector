package io.github.lstramke.coincollector.export;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;

public class ExportServiceImplTest {
    
    @FunctionalInterface
    interface MockSetup {
        void setup(EuroCoinCollectionGroupStorageService service, List<ExportWriter> writers) throws Exception;
    }

    private static final byte[] TEST_CONTENT = "Test export content".getBytes(StandardCharsets.UTF_8);

    private record ExportTestcase(
        UUID userId,
        ExportFormat exportFormat,
        List<ExportFormat> registeredExportFormats,
        MockSetup mockSetup,
        ExportResult expectedResult,
        Class<? extends Exception> expectedException,
        String description
    ) {
        @Override
        public String toString(){
            return description;
        }
    }

    private static Stream<ExportTestcase> exportTestcases(){
        return Stream.of(
            new ExportTestcase(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                ExportFormat.CSV,
                List.of(ExportFormat.values()),
                (service, writers) -> {
                    when(service.getAllByUser(
                        "11111111-1111-1111-1111-111111111111"
                    )).thenReturn(List.of());

                    ExportWriter csvWriter = writers.stream()
                        .filter(writer -> writer.format() == ExportFormat.CSV)
                        .findFirst()
                        .orElseThrow();

                    when(csvWriter.write(anyList())).thenReturn(TEST_CONTENT);
                },
                new ExportResult(TEST_CONTENT, ExportFormat.CSV),
                null,
                "successful csv export"
            ),
            new ExportTestcase(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                ExportFormat.JSON,
                 List.of(ExportFormat.values()),
                (service, writers) -> {
                    when(service.getAllByUser(
                        "11111111-1111-1111-1111-111111111111"
                    )).thenReturn(List.of());

                    ExportWriter csvWriter = writers.stream()
                        .filter(writer -> writer.format() == ExportFormat.JSON)
                        .findFirst()
                        .orElseThrow();

                    when(csvWriter.write(anyList())).thenReturn(TEST_CONTENT);
                },
                new ExportResult(TEST_CONTENT, ExportFormat.JSON),
                null,
                "successful json export"
            ),
            new ExportTestcase(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                ExportFormat.JSON,
                 List.of(ExportFormat.values()),
                (service, writers) -> {
                    when(service.getAllByUser(
                        "11111111-1111-1111-1111-111111111111"
                    )).thenThrow(EuroCoinCollectionGroupGetAllException.class);
                },
                null,
                EuroCoinCollectionGroupGetAllException.class,
                "storage service throws on getAllByUserId"
            ),
            new ExportTestcase(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                ExportFormat.JSON,
                 List.of(ExportFormat.CSV),
                (service, writers) -> {
                    when(service.getAllByUser(
                        "11111111-1111-1111-1111-111111111111"
                    )).thenReturn(List.of());
                },
                null,
                NullPointerException.class,
                "storage service throws on getAllByUserId"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("exportTestcases")
    void testExport(ExportTestcase testcase) throws Exception{
        EuroCoinCollectionGroupStorageService storageService = mock(EuroCoinCollectionGroupStorageService.class);
        List<ExportWriter> writers = testcase.registeredExportFormats().stream()
            .map(format -> {
                ExportWriter writer = mock(ExportWriter.class);
                when(writer.format()).thenReturn(format);
                return writer;
            })
            .toList();
        
        testcase.mockSetup().setup(storageService, writers);

        ExportService exportService = new ExportServiceImpl(writers, storageService);

        if (testcase.expectedException != null) {
            assertThrows(testcase.expectedException, () -> exportService.export(testcase.userId(), testcase.exportFormat()));
        } else {
            var result = assertDoesNotThrow(() -> exportService.export(testcase.userId(), testcase.exportFormat()));
            assertEquals(testcase.expectedResult, result);
            verify(storageService, times(1)).getAllByUser(testcase.userId().toString());
            writers.forEach(writer ->
                verify(writer, writer.format() == testcase.exportFormat() ? times(1) : never()).write(anyList())
            );
        }
    }
} 
