package io.github.lstramke.coincollector.export;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException.EuroCoinCollectionGroupGetAllException;
import io.github.lstramke.coincollector.services.EuroCoinCollectionGroupStorageService;

@Service 
public class ExportServiceImpl implements ExportService {

    private final Map<ExportFormat, ExportWriter> writers;
    private final EuroCoinCollectionGroupStorageService groupStorageService;

    public ExportServiceImpl(List<ExportWriter> writers, EuroCoinCollectionGroupStorageService groupStorageService) {
        this.writers = new EnumMap<>(ExportFormat.class);

        for (ExportWriter exportWriter : writers) {
            var previous = this.writers.put(exportWriter.format(), exportWriter);

            if(previous != null) {
                throw new IllegalStateException(
                    "Multiple writers registered for format: " + exportWriter.format()
                );
            }
        }

        this.groupStorageService = groupStorageService;
    }

    @Override
    public ExportResult export(UUID userId, ExportFormat exportFormat) throws EuroCoinCollectionGroupGetAllException {
        var writer = writers.get(exportFormat);
        if (writer == null) {
            throw new UnsupportedOperationException("Export format " + exportFormat + " is not supported via a registered writer");
        }
        
        var wholeCollection = this.groupStorageService.getAllByUser(userId.toString());
        byte[] content = writer.write(wholeCollection);
        return new ExportResult(
            content,
            exportFormat
        );
    }
    
}
