package io.github.lstramke.coincollector.model;

import java.util.List;

public class EuroCoin implements CoinComponent {
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
    
    public String getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public CoinValue getValue() {
        return value;
    }

    public CoinCountry getMintCountry() {
        return mintCountry;
    }

    public Mint getMint() {
        return mint;
    }

    public CoinDescription getDescription() {
        return description;
    }

    public void setDescription(CoinDescription description) {
        this.description = description;
    }

    @Override
    public Boolean isComplete() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isComplete'");
    }

    @Override
    public int getTotalValue() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTotalValue'");
    }

    @Override
    public int getCoinCount() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCoinCount'");
    }

    @Override
    public List<EuroCoin> getAllCoins() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllCoins'");
    }
}
