package io.github.lstramke.coincollector.model.DTOs.Exports;

import java.util.List;
import java.util.Objects;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;

public record EuroCoinCollectionGroupExportDto(
    String name,
    List<EuroCoinCollectionExportDto> collections
) {
    public static EuroCoinCollectionGroupExportDto from(EuroCoinCollectionGroup group) {
        Objects.requireNonNull(group, "group must be not null");
        return new EuroCoinCollectionGroupExportDto(
            group.getName(), 
            group.getCollections().stream()
                .map(EuroCoinCollectionExportDto::from)
                .toList()
        );
    }
}
