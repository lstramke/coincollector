package io.github.lstramke.coincollector.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;

import io.github.lstramke.coincollector.export.ExportFormat;
import io.github.lstramke.coincollector.export.ExportWriter;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.DTOs.Exports.EuroCoinCollectionGroupExportDto;

@Component 
public class OdsExportWriter implements ExportWriter {

    @Override
    public ExportFormat format() {
        return ExportFormat.ODS;
    }

    @Override
    public byte[] write(List<EuroCoinCollectionGroup> wholeCollection) {
        try (var output = new ByteArrayOutputStream()) {
            OdfSpreadsheetDocument document;
            try {
                document = OdfSpreadsheetDocument.newSpreadsheetDocument();
            } catch (Exception e) {
                throw new IllegalStateException("ODS document could not be created", e);
            }
            
            var groups = wholeCollection.stream()
                .map(EuroCoinCollectionGroupExportDto::from)
                .toList();

            for (int index = 0; index < groups.size(); index++) {
                var group = groups.get(index);
                var sheet = index == 0
                    ? document.getTableList().get(0)
                    : appendSheet(document, group.name());

                sheet.setTableName(group.name());
                writeGroup(sheet, group);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("ODS export failed", e);
        } catch (Exception e) {
            throw new IllegalStateException("ODS document could not be written", e);
        } 
    }
    
    private OdfTable appendSheet(OdfSpreadsheetDocument document, String sheetName) {
        OdfTable sheet = OdfTable.newTable(document);
        sheet.setTableName(sheetName);
        return sheet;
    }
    
    private void writeRow(OdfTable sheet, String... values) {
        OdfTableRow row = sheet.appendRow();
        for (int column = 0; column < values.length; column++) {
            OdfTableCell cell = row.getCellByIndex(column);
            cell.setStringValue(values[column]);
        }
    }

    private void writeGroup(OdfTable sheet, EuroCoinCollectionGroupExportDto group) {
        writeRow(sheet, group.name());

        for (var collection : group.collections()) {
            writeRow(sheet, collection.name());
            writeRow(sheet, "", "Jahr", "Nennwert", "Prägeland",
                "Prägestätte", "Beschreibung");

            for (var coin : collection.coins()) {
                writeRow(
                    sheet,
                    "",
                    Integer.toString(coin.year()),
                    coin.value(),
                    coin.mintCountry(),
                    coin.mint().equals("UNKNOWN") ? "" : coin.mint(),
                    coin.description()
                );
            }

            writeRow(sheet);
        }
    }
}
