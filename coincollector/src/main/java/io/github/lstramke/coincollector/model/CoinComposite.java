package io.github.lstramke.coincollector.model;

import java.util.List;

public interface CoinComposite extends CoinComponent{
    void add(CoinComponent component);
    void remove(CoinComponent component);
    List<CoinComponent> getChildren();
    void addCoin(EuroCoin coin);
    void addCollection(CoinCollection collection);
}
