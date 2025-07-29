package io.github.lstramke.coincollector.model;

public class EuroCoinBuilder {
    int year;
    CoinValue value;
    CoinCountry mintCountry;
    CoinDescription description;
    Mint mint;
    String id;

    public EuroCoinBuilder setYear(int year) {
        this.year = year;
        return this;
    }

    public EuroCoinBuilder setValue(CoinValue value) {
        this.value = value;
        return this;
    }

    public EuroCoinBuilder setMintCountry(CoinCountry mintCountry) {
        this.mintCountry = mintCountry;
        return this;
    }

    public EuroCoinBuilder setDescription(CoinDescription description) {
        this.description = description;
        return this;
    }

    public EuroCoinBuilder setMint(Mint mint) {
        this.mint = mint;
        return this;
    }

    public EuroCoin build() throws IllegalStateException {
        if (year <= 0) {
            throw new IllegalStateException("Year must be greater than 0");
        }
        if (value == null) {
            throw new IllegalStateException("CoinValue cannot be null");
        }
        if (mintCountry == null) {
            throw new IllegalStateException("MintCountry cannot be null");
        }
        if (description == null) {
            this.description = new CoinDescription(this.value, this.year, this.mintCountry, this.mint);
        }
        
        this.id = generateId();
        
        return new EuroCoin(this);
    }

    private String generateId() {
        return String.format("%s_%s_%d_%s_%s", 
            mintCountry != null ? mintCountry.name() : "UNKNOWN",
            value != null ? value.name() : "UNKNOWN", 
            year,
            mint != null ? mint.name() : "UNKNOWN",
            description != null ? description.getText().replaceAll("\\s+", "_").toUpperCase() : "NO_DESC"
        );
    }
}
