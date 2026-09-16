package io.github.lstramke.coincollector.model;

import java.util.UUID;

/**
 * Concrete implementation of a Euro coin including mint country, mint mark and
 * optional generated description.
 */
public class EuroCoin implements Coin {
    private final UUID id;
    private final EuroCoinType type;
    private CoinDescription description;
    private String collectionId;

    /**
     * Package-private constructor used exclusively by the builder/factory.
     * Coins should be created via {@link EuroCoinBuilder#build()} (or
     * {@link EuroCoinFactory} for hydration/import) to ensure validation,
     * deterministic id handling and consistent description generation.
     * Not intended for direct use in application code.
     */
    EuroCoin(EuroCoinBuilder builder){
        this.id = builder.id;
        this.type = new EuroCoinType(builder.year, builder.value, builder.mintCountry, builder.mint);
        this.description = builder.description;
        this.collectionId = builder.collectionId;
    }

    @Override
    public String toString() {
        return "EuroCoin [id=" + id + ", year=" + type.year() + ", value=" + type.value() + ", mintCountry=" + type.mintCountry() + ", mint="
                + type.mint() + ", description=" + description + ", collectionId=" + collectionId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((collectionId == null) ? 0 : collectionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EuroCoin other = (EuroCoin) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (collectionId == null) {
            if (other.collectionId != null)
                return false;
        } else if (!collectionId.equals(other.collectionId))
            return false;
        return true;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public int getYear() {
        return type.year();
    }

    @Override
    public CoinValue getValue() {
        return type.value();
    }

    @Override
    public CoinDescription getDescription() {
        return description;
    }

    @Override
    public void setDescription(CoinDescription description) {
        this.description = description;
    }

    @Override
    public String getCollectionId() {
        return collectionId;
    }

    public CoinCountry getMintCountry() {
        return type.mintCountry();
    }

    public Mint getMint() {
        return type.mint();
    }

    public EuroCoinType getType() {
        return this.type;
    }
}
