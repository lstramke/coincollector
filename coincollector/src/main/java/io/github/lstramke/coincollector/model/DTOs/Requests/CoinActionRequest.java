package io.github.lstramke.coincollector.model.DTOs.Requests;

public record CoinActionRequest(
    int year,
    int value, 
    String country,
    String collectionId,
    String mint,
    String description
) {
    
}
