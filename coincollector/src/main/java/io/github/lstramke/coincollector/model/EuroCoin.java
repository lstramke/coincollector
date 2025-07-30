package io.github.lstramke.coincollector.model;

public class EuroCoin implements Coin {
    private final String id;
    private final int year;
    private final CoinValue value;
    private final CoinCountry mintCountry;
    private final Mint mint;
    private CoinDescription description;
    private String collectionId;

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
