package io.github.lstramke.coincollector.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Concrete collection of {@link EuroCoin} with aggregation helpers.
 */
public class EuroCoinCollection implements CoinCollection<EuroCoin> {
    private final String id;
    private String name;
    private final List<EuroCoin> coins;
    private String groupId;

    /**
     * Create an empty collection with a newly generated random id.
     */
    public EuroCoinCollection(String name, String groupId) {
        this(createCollectionId(), name, List.of(), groupId);
    }

    /**
     * Create a collection with initial coins (defensive copy) and a newly
     * generated random id.
     */
    public EuroCoinCollection(String name, List<EuroCoin> coins, String groupId) {
        this(createCollectionId(), name, coins, groupId);
    }

    /**
     * Canonical constructor performing validation of id, groupId and coins.
     * All other constructors delegate to this one.
     * @param id existing unique collection id (must not be null/blank)
     * @param name display name (may be null if allowed by domain)
     * @param coins initial coins (non-null list, no null elements; defensively copied)
     * @param groupId owning group id (must not be null/blank)
     * @throws IllegalArgumentException if validation fails
     */
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

    /**
     * Package-private constructor intended for factories inside this model
     * package to preserve an existing id without generating a new one.
     * Delegates to the private canonical constructor.
     */
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
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(String newGroupId) {
        if (newGroupId == null || newGroupId.isBlank()) {
            throw new IllegalArgumentException("newGroupId is null or blank");
        }
        this.groupId = newGroupId;
    }

    @Override
    public List<EuroCoin> getCoins() {
        return List.copyOf(coins);
    }

    @Override
    public void addCoin(EuroCoin coin) {
        if (coin == null)
            return;
        // TODO: coin collcetion id update
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EuroCoinCollection other = (EuroCoinCollection) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (coins == null) {
            if (other.coins != null)
                return false;
        } else if (!coins.equals(other.coins))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        return true;
    }
}
