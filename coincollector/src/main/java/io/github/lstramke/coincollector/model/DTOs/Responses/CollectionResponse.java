package io.github.lstramke.coincollector.model.DTOs.Responses;

import java.util.List;

import io.github.lstramke.coincollector.model.EuroCoinCollection;

public record CollectionResponse(
    String id,
    String name,
    String groupId,
    List<CoinResponse> coins
) {
    
    public static CollectionResponse fromDomain(EuroCoinCollection collection) {
        return new CollectionResponse(
            collection.getId(),
            collection.getName(),
            collection.getGroupId(),
            collection.getCoins().stream()
                .map(CoinResponse::fromDomain)
                .toList()
        );
    }
}
