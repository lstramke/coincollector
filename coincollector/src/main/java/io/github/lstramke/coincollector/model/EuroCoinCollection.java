package io.github.lstramke.coincollector.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EuroCoinCollection implements CoinCollection<EuroCoin>{
    private final String id;
    private String name;
    private final List<EuroCoin> coins;

    public EuroCoinCollection(String name){
        this(idFromUUID(), name, List.of());
    }

    public EuroCoinCollection(String name, List<EuroCoin> coins){
        this(idFromUUID(), name, coins);
    }

    private EuroCoinCollection(String id, String name, List<EuroCoin> coins){
        this.id = id;
        this.name = name;
        this.coins = new ArrayList<>(coins == null ? List.of() : coins);
    }

    private static String idFromUUID() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public List<EuroCoin> getCoins() {
        return List.copyOf(coins);
    }

    @Override
    public void addCoin(EuroCoin coin) {
        if(coin == null) return;
       this.coins.add(coin);
    }

    @Override
    public void removeCoin(EuroCoin coin) {
        if(coin == null) return;
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
