package io.github.lstramke.coincollector.model;

import java.util.List;

public interface CoinComponent {
    String getId();
    CoinDescription getDescription();
    Boolean isComplete();
    int getTotalValue();
    int getCoinCount();
    List<EuroCoin> getAllCoins();
}
