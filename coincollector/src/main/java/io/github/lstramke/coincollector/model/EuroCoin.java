package io.github.lstramke.coincollector.model;

/**
 * Concrete implementation of a Euro coin including mint country, mint mark and
 * optional generated description.
 */
public class EuroCoin implements Coin {
    private final String id;
    private final int year;
    private final CoinValue value;
    private final CoinCountry mintCountry;
    private final Mint mint;
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
        this.year = builder.year;
        this.value = builder.value;
        this.mintCountry = builder.mintCountry;
        this.mint = builder.mint;
        this.description = builder.description;
        this.collectionId = builder.collectionId;
    }

    @Override
    public String toString() {
        return "EuroCoin [id=" + id + ", year=" + year + ", value=" + value + ", mintCountry=" + mintCountry + ", mint="
                + mint + ", description=" + description + ", collectionId=" + collectionId + "]";
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
        if (year != other.year)
            return false;
        if (value != other.value)
            return false;
        if (mintCountry != other.mintCountry)
            return false;
        if (mint != other.mint)
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
    public String getId() {
        return id;
    }

    @Override
    public int getYear() {
        return year;
    }

    @Override
    public CoinValue getValue() {
        return value;
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
        return mintCountry;
    }

    public Mint getMint() {
        return mint;
    }
}
