package io.github.lstramke.coincollector.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.export.ExportFormat;
import io.github.lstramke.coincollector.export.ExportResult;
import io.github.lstramke.coincollector.export.ExportService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
@RequestMapping("/api/v1/export")
public class ExportHandler {

    private final static Logger logger = LoggerFactory.getLogger(ExportHandler.class);
    private final ExportService exportService;

    public ExportHandler(ExportService exportService) {
        this.exportService = exportService;
    }
    
    @GetMapping
    public ResponseEntity<byte[]> exportWholeCollection(@RequestParam String fileType, Authentication authentication) {
        logger.info("Collection export with fileType: {} requested", fileType);
        ExportFormat format = ExportFormat.from(fileType)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported export format"));

        var userId = requireUserId(authentication);
        
        ExportResult exportResult;
        try {
            exportResult = this.exportService.export(UUID.fromString(userId), format);
        } catch (EuroCoinCollectionGroupGetAllException | UnsupportedOperationException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(exportResult.contentType().mimeType()))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("collection." + exportResult.contentType().fileExtension())
                    .build()
                    .toString()
            )
            .body(exportResult.content());
    }

    /**
     * Extracts the user ID from authentication.
     *
     * @param authentication the Spring Security authentication
     * @return the user ID
     * @throws ResponseStatusException if authentication is missing
     */
    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Collection request failed: authentication missing");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authentication.getPrincipal().toString();
    }

}
