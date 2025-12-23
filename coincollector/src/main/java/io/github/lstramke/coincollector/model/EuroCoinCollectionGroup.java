package io.github.lstramke.coincollector.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Concrete implementation of a {@link CollectionGroup} for Euro coin
 * collections owned by a single user.
 */
public class EuroCoinCollectionGroup implements CollectionGroup<EuroCoin, EuroCoinCollection> {
    private String id;
    private String name;
    private String ownerId;
    private List<EuroCoinCollection> collections;

    /**
     * Create an empty group with a newly generated random id.
     */
    public EuroCoinCollectionGroup(String name, String ownerId){
        this(createGroupId(), name, ownerId, List.of());
    }

    /**
     * Create a group with initial collections and a newly generated random id.
     */
    public EuroCoinCollectionGroup(String name, String ownerId, List<EuroCoinCollection> collections){
        this(createGroupId(), name, ownerId, collections);
    }

    /**
     * Canonical constructor performing validation of id, ownerId and collections.
     * All other constructors delegate to this one.
     * @param id existing unique group id (must not be null/blank)
     * @param name display name (may be null if allowed by domain)
     * @param ownerId owner user id (must not be null/blank)
     * @param collections initial collections (non-null, no null elements)
     * @throws IllegalArgumentException if validation fails
     */
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
    
    /**
     * Package-private constructor intended for factories inside this model
     * package to preserve an existing id without generating a new one.
     * Delegates to the private canonical constructor.
     */
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
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public String getOwnerId(){
        return ownerId;
    }

    @Override
    public List<EuroCoinCollection> getCollections() {
        return List.copyOf(collections);
    }

    @Override
    public void addCollection(EuroCoinCollection collection) {
        if (collection == null || !collection.getGroupId().equals(this.id))
            throw new IllegalArgumentException("collection is null or has false groupId");
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
