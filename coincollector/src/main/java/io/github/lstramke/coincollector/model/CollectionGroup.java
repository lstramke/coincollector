package io.github.lstramke.coincollector.model;

import java.util.List;

public interface CollectionGroup<T extends Coin, C extends CoinCollection<T>> {
    String getId();
    String getName();
    String getOwnerId();
    List<C> getCollections();
    void addCollection(C collection);
    void removeCollection(C collection);
    int getTotalCollections();
    int getTotalCoins();
    int getTotalValue();
}
