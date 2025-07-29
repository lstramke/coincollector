package io.github.lstramke.coincollector.model;

public class EuroCoin implements Coin {
    private final String id;
    private final int year;
    private final CoinValue value;
    private final CoinCountry mintCountry;
    private final Mint mint;
    private CoinDescription description;

    EuroCoin(EuroCoinBuilder builder){
        this.id = builder.id;
        this.year = builder.year;
        this.value = builder.value;
        this.mintCountry = builder.mintCountry;
        this.mint = builder.mint;
        this.description = builder.description;
    }

    @Override
    public String toString() {
        return String.format("EuroCoin[id='%s', year=%d, value=%s, mintCountry=%s, mint=%s, description='%s']", 
            this.id, this.year, this.value, this.mintCountry, this.mint, this.getDescription());
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

    public CoinCountry getMintCountry() {
        return mintCountry;
    }

    public Mint getMint() {
        return mint;
    }

    @Override
    public CoinDescription getDescription() {
        return description;
    }

    public void setDescription(CoinDescription description) {
        this.description = description;
    }
}
