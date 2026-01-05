package io.github.lstramke.coincollector.model.DTOs.Responses;

import io.github.lstramke.coincollector.model.EuroCoin;

public record CoinResponse(
    String id,
    int year,
    int value,
    String country,
    String mint,
    String description,
    String collectionId
){
    
    public static CoinResponse fromDomain(EuroCoin coin) {
        return new CoinResponse(
            coin.getId(),
            coin.getYear(),
            coin.getValue().getCentValue(),
            coin.getMintCountry().getIsoCode(),
            coin.getMint() != null ? coin.getMint().getMintMark() : null,
            coin.getDescription().toString(),
            coin.getCollectionId()
        );
    }
}
