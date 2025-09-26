package io.github.lstramke.coincollector.model;

import java.util.List;

/**
 * A container holding coins of a specific subtype. Implementations manage
 * adding, removing and aggregating coin data.
 * @param <T> concrete coin type
 */
public interface CoinCollection<T extends Coin> {
    String getId();
    String getName();
    void setName(String newName);
    void setGroupId(String newGroupId);
	String getGroupId();
    List<T> getCoins();
    void addCoin(T coin);
    void removeCoin(T coin);
    int getTotalValue();
    int getCoinCount();

}
