package io.github.lstramke.coincollector.model;

/**
 * Represents a generic coin with a value, minting year, description and the
 * identifier of the collection it belongs to.
 */
public interface Coin {
    String getId();
    CoinValue getValue();
    int getYear();
    CoinDescription getDescription();
    void setDescription(CoinDescription description);
    String getCollectionId();
}
