package io.github.lstramke.coincollector.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EuroCoinCollection implements CoinCollection<EuroCoin> {
    private final String id;
    private String name;
    private final List<EuroCoin> coins;
    private String groupId;

    public EuroCoinCollection(String name, String groupId) {
        this(createCollectionId(), name, List.of(), groupId);
    }

    public EuroCoinCollection(String name, List<EuroCoin> coins, String groupId) {
        this(createCollectionId(), name, coins, groupId);
    }

    private EuroCoinCollection(String id, String name, List<EuroCoin> coins, String groupId) throws IllegalArgumentException {
        if (id == null || id.isBlank()){
            throw new IllegalArgumentException("id is null or blank");
        }
        this.id = id;
        
        this.name = name;

        if (groupId == null || groupId.isBlank()){
            throw new IllegalArgumentException("groupId is null or blank");
        }
        this.groupId = groupId;

        final List<EuroCoin> src = (coins == null ? List.of() : coins);
        if (src.stream().anyMatch(c -> c == null)) {
            throw new IllegalArgumentException("coins list contains null element(s)");
        }
        this.coins = new ArrayList<>(src);
    }

    EuroCoinCollection(String id, String name, String groupId) {
        this(id, name, List.of(), groupId);
    }

    private static String createCollectionId() {
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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String newGroupId) {
        if (newGroupId == null || newGroupId.isBlank()) {
            throw new IllegalArgumentException("newGroupId is null or blank");
        }
        this.groupId = newGroupId;
    }

    @Override
    public void addCoin(EuroCoin coin) {
        if (coin == null)
            return;
        this.coins.add(coin);
    }

    @Override
    public void removeCoin(EuroCoin coin) {
        if (coin == null)
            return;
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
