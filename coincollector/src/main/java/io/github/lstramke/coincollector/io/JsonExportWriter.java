package io.github.lstramke.coincollector.io;

import java.util.List;

import org.springframework.stereotype.Component;

import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.ObjectMapper;

import io.github.lstramke.coincollector.export.ExportFormat;
import io.github.lstramke.coincollector.export.ExportWriter;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.DTOs.Exports.EuroCoinCollectionGroupExportDto;

@Component 
public class JsonExportWriter implements ExportWriter{

    private final ObjectMapper objectMapper;

    public JsonExportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ExportFormat format() {
        return ExportFormat.JSON;
    }

    @Override
    public byte[] write(List<EuroCoinCollectionGroup> wholeCollection) {
        var exportData = wholeCollection.stream()
            .map(EuroCoinCollectionGroupExportDto::from)
            .toList();
        return objectMapper.writer().with(createExportPrettyPrinter()).writeValueAsBytes(exportData);
    }

    private DefaultPrettyPrinter createExportPrettyPrinter() {
        var indenter = new DefaultIndenter("  ", "\n");

        var separators = Separators.createDefaultInstance()
            .withObjectNameValueSpacing(Separators.Spacing.AFTER)
            .withArrayEmptySeparator("")
            .withObjectEmptySeparator("");

        return new DefaultPrettyPrinter()
            .withSeparators(separators)
            .withObjectIndenter(indenter)
            .withArrayIndenter(indenter);
    }
    
}
