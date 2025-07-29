package io.github.lstramke.coincollector.model;

import java.util.List;

public class EuroCoinCollectionGroup implements CollectionGroup<EuroCoin, EuroCoinCollection> {
    private String id;
    private String name;
    private String ownerID;
    private List<EuroCoinCollection> collections;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOwnerId(){
        return ownerID;
    }

    public List<EuroCoinCollection> getCollections() {
        return collections;
    }

    @Override
    public void addCollection(EuroCoinCollection collection) {
        this.collections.add(collection);
    }

    @Override
    public void removeCollection(EuroCoinCollection collection) {
        this.collections.remove(collection);
    }

    @Override
    public int getTotalCollections() {
        return collections.size();
    }

    @Override
    public int getTotalCoins() {
        return collections.stream().mapToInt(collection -> collection.getCoinCount()).sum();
    }

    @Override
    public int getTotalValue() {
        return collections.stream().mapToInt(collection -> collection.getTotalValue()).sum();
    }
}
