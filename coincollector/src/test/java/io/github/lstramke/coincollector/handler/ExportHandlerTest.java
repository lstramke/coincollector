package io.github.lstramke.coincollector.handler;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.export.ExportFormat;
import io.github.lstramke.coincollector.export.ExportResult;
import io.github.lstramke.coincollector.export.ExportService;
import io.github.lstramke.coincollector.security.SecurityConfig;
import io.github.lstramke.coincollector.services.SessionManager;

@WebMvcTest(value = ExportHandler.class)
@Import(SecurityConfig.class)
public class ExportHandlerTest {

    @MockitoBean 
    ExportService exportService;

    @MockitoBean
    SessionManager sessionManager;

    @Autowired
    MockMvc mockMvc;

    @FunctionalInterface 
    private interface MockSetup {
        void setup(ExportService exportService, SessionManager sessionManager) throws Exception;
    }

    private static final String PREFIX = "/api/v1/export";
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SESSION_ID = "session-abc";

    private record ExportWholeCollectionTestcase(
        String method,
        String path,
        String fileType,
        MockSetup mockSetup,
        int expectedStatus,
        byte[] expectedExport,
        String description
    ) {
        @Override 
        public String toString(){
            return description;
        }
    }

    private static Stream<ExportWholeCollectionTestcase> exportWholeCollectionTestcases(){
        byte[] csvContent = (
            "id,name\n" +
            "test,test-coin\n"
        ).getBytes(StandardCharsets.UTF_8);

        byte[] jsonContent = """
            [{"id":test,"name":"test-coin"}]
            """.getBytes(StandardCharsets.UTF_8);

        return Stream.of(
            new ExportWholeCollectionTestcase(
                "GET",
                PREFIX,
                "json",
                (exportService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                    when(exportService.export(eq(UUID.fromString(USER_ID)), any())).thenReturn(new ExportResult(jsonContent, ExportFormat.JSON));
                },
                200,
                jsonContent,
                "happy path json"

            ),
            new ExportWholeCollectionTestcase(
                "GET",
                PREFIX,
                "csv",
                (exportService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                    when(exportService.export(eq(UUID.fromString(USER_ID)), any())).thenReturn(new ExportResult(csvContent, ExportFormat.CSV));
                },
                200,
                csvContent,
                "happy path csv"

            ),
            new ExportWholeCollectionTestcase(
                "GET",
                PREFIX,
                "json",
                (exportService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID))
                        .thenReturn(false);
                },
                401,
                new byte[0],
                "session rejected"
            ),
            new ExportWholeCollectionTestcase(
                "GET",
                PREFIX,
                "xml",
                (exportService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID))
                        .thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID))
                        .thenReturn(USER_ID);
                },
                400,
                new byte[0],
                "unsupported file format"
            ),
            new ExportWholeCollectionTestcase(
                "GET",
                PREFIX,
                "json",
                (exportService, sessionManager) -> {
                    when(sessionManager.validateSession(SESSION_ID)).thenReturn(true);
                    when(sessionManager.getUserId(SESSION_ID)).thenReturn(USER_ID);
                    when(exportService.export(eq(UUID.fromString(USER_ID)), any())).thenThrow(EuroCoinCollectionGroupGetAllException.class);
                },
                500,
                new byte[0],
                "happy path json"

            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("exportWholeCollectionTestcases")
    void testExportWholeCollection(ExportWholeCollectionTestcase testcase) throws Exception {
        testcase.mockSetup().setup(exportService, sessionManager);

        var actionResult = mockMvc.perform(
            get(testcase.path)
            .param("fileType", testcase.fileType())
            .cookie(new Cookie("sessionId", SESSION_ID))
        ).andExpect(status().is(testcase.expectedStatus));

        if (testcase.expectedStatus == 200) {
            var format = ExportFormat.from(testcase.fileType()).get();
            actionResult.andExpect(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    containsString("attachment")
                )
            ).andExpect(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    containsString("collection." + testcase.fileType())
                )
            ).andExpectAll(
                content().bytes(testcase.expectedExport()),
                content().contentType(format.mimeType())
            );
            verify(exportService, times(1)).export(UUID.fromString(USER_ID), format);
        }
        else if (testcase.expectedStatus == 401) {
            verify(exportService, never()).export(any(), any());
        }
        
    }
    
}
