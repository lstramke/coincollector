package io.github.lstramke.coincollector.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EuroCoinCollectionGroup implements CollectionGroup<EuroCoin, EuroCoinCollection> {
    private String id;
    private String name;
    private String ownerId;
    private List<EuroCoinCollection> collections;

    public EuroCoinCollectionGroup(String name, String ownerId){
        this(createGroupId(), name, ownerId, List.of());
    }

    public EuroCoinCollectionGroup(String name, String ownerId, List<EuroCoinCollection> collections){
        this(createGroupId(), name, ownerId, collections);
    }

    private EuroCoinCollectionGroup(String id, String name, String ownerId, List<EuroCoinCollection> collections) throws IllegalArgumentException{
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id is null or blank");
        }
        this.id = id;

        if(ownerId == null || ownerId.isBlank()){
            throw new IllegalArgumentException("ownerId is null or blank");
        }
        this.ownerId = ownerId;

        if(collections == null || collections.stream().anyMatch(c -> c == null)){
            throw new IllegalArgumentException("collections list is null");
        }
        this.collections = new ArrayList<>(collections);
        this.name = name;
    }
    
    EuroCoinCollectionGroup(String id, String name, String ownerId){
        this(id, name, ownerId, List.of());
    }

    private static String createGroupId() {
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
    public String getOwnerId(){
        return ownerId;
    }

    public List<EuroCoinCollection> getCollections() {
        return List.copyOf(collections);
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
