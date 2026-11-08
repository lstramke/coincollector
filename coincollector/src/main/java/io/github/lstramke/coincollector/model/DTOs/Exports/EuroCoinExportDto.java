package io.github.lstramke.coincollector.model.DTOs.Exports;

import java.util.Objects;

import io.github.lstramke.coincollector.model.EuroCoin;

public record EuroCoinExportDto(
    int year,
    String value,
    String mintCountry,
    String mint,
    String description
) {
    public static EuroCoinExportDto from(EuroCoin coin) {
        Objects.requireNonNull(coin, "coin must not be null");
        return new EuroCoinExportDto(
            coin.getYear(), 
            coin.getValue().getDisplayName(), 
            coin.getMintCountry().getDisplayName(),
            coin.getMint().getMintMark(),
            coin.getDescription().toString()
        );
    }
}
