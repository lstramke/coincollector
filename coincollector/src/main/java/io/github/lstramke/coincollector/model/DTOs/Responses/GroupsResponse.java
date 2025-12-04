package io.github.lstramke.coincollector.model.DTOs.Responses;

import java.util.List;

import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;

public record GroupsResponse(
    String id,
    String name,
    List<CollectionResponse> collections
) {
    
    public static GroupsResponse fromDomain(EuroCoinCollectionGroup group) {
        return new GroupsResponse(
            group.getId(),
            group.getName(),
            group.getCollections().stream()
                .map(CollectionResponse::fromDomain)
                .toList()
        );
    }
}
