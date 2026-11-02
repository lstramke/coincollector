package io.github.lstramke.coincollector.model.DTOs.Exports;

import java.util.Objects;

public record ExportCsvRow(
    String groupName,
    String collectionName,
    int year,
    String value,
    String mintCountry,
    String mint,
    String description
) {
    public static ExportCsvRow from(
        EuroCoinCollectionGroupExportDto group, 
        EuroCoinCollectionExportDto collection, 
        EuroCoinExportDto coin
    ) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(collection, "collection must not be null");
        Objects.requireNonNull(coin, "coin must not be null");

        return new ExportCsvRow(
            group.name(), 
            collection.name(), 
            coin.year(), 
            coin.value(), 
            coin.mintCountry(), 
            coin.mint().equals("UNKNOWN") ? "" : coin.mint(), 
            coin.description()
        );
    }
}
