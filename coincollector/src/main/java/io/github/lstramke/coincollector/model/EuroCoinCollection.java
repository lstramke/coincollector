package io.github.lstramke.coincollector.model;

import java.util.List;

public class EuroCoinCollection implements CoinCollection<EuroCoin>{
    private String id;
    private String name;
    private List<EuroCoin> coins;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<EuroCoin> getCoins() {
        return coins;
    }

    @Override
    public void addCoin(EuroCoin coin) {
       this.coins.add(coin);
    }

    @Override
    public void removeCoin(EuroCoin coin) {
        this.coins.remove(coin);
    }

    @Override
    public int getTotalValue() {
        return coins.stream().mapToInt(coin -> coin.getValue().getCentValue()).sum();
    }

    @Override
    public int getCoinCount() {
        return coins.size();
    }
}
