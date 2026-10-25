package io.github.lstramke.coincollector.io;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.springframework.stereotype.Component;

import io.github.lstramke.coincollector.export.ExportFormat;
import io.github.lstramke.coincollector.export.ExportWriter;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.DTOs.Exports.EuroCoinCollectionGroupExportDto;
import io.github.lstramke.coincollector.model.DTOs.Exports.ExportCsvRow;

@Component 
public class CsvExportWriter implements ExportWriter{

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
        .setHeader(
            "Gruppe",
            "Sammlung",
            "Jahr",
            "Nennwert",
            "Prägeland",
            "Prägestätte",
            "Beschreibung"
        )
        .get();

    @Override
    public ExportFormat format() {
        return ExportFormat.CSV;
    }

    @Override
    public byte[] write(List<EuroCoinCollectionGroup> wholeCollection) {
        var exportData = wholeCollection.stream()
            .map(EuroCoinCollectionGroupExportDto::from);

            
        var rows = exportData
            .flatMap(group -> group.collections().stream()
                .flatMap(collection -> collection.coins().stream()
                    .map(coin -> ExportCsvRow.from(group, collection, coin))
                )
            );
    
        var output = new StringWriter();
        var rowIterator = rows.iterator();
        try (var csv = CSV_FORMAT.print(output)) {
            while(rowIterator.hasNext()){
                var row = rowIterator.next();
                csv.printRecord(
                    row.groupName(),
                    row.collectionName(),
                    row.year(),
                    row.value(),
                    row.mintCountry(),
                    row.mint(),
                    row.description()
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException("CSV export failed", e);
        }
        
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

}
