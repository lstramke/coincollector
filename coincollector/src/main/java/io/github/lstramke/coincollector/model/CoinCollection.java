package io.github.lstramke.coincollector.model;

import java.util.List;

public interface CoinCollection<T extends Coin> {
    String getId();
    String getName();
    List<T> getCoins();
    void addCoin(T coin);
    void removeCoin(T coin);
    int getTotalValue();
    int getCoinCount();
}
