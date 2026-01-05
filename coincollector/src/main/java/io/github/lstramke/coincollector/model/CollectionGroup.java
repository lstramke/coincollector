package io.github.lstramke.coincollector.model;

import java.util.List;

/**
 * Logical grouping of multiple coin collections owned by a user.
 * @param <T> coin type
 * @param <C> collection type
 */
public interface CollectionGroup<T extends Coin, C extends CoinCollection<T>> {
    String getId();
    String getName();
    void setName(String newName);
    String getOwnerId();
    List<C> getCollections();
    void addCollection(C collection);
    void removeCollection(C collection);
    int getTotalCollections();
    int getTotalCoins();
    int getTotalValue();
}
