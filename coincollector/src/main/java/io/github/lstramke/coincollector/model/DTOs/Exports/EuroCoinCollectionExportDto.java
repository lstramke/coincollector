package io.github.lstramke.coincollector.model.DTOs.Exports;

import java.util.List;
import java.util.Objects;

import io.github.lstramke.coincollector.model.EuroCoinCollection;

public record EuroCoinCollectionExportDto(
    String name,
    List<EuroCoinExportDto> coins
) {
    public static EuroCoinCollectionExportDto from(EuroCoinCollection collection) {
        Objects.requireNonNull(collection, "collection must not be null");
        return new EuroCoinCollectionExportDto(
            collection.getName(),
            collection.getCoins().stream()
                .map(EuroCoinExportDto::from)
                .toList()
        );
    }
}
