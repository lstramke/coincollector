package io.github.lstramke.coincollector.model;

public interface Coin {
    String getId();
    CoinValue getValue();
    int getYear();
    CoinDescription getDescription();
}
