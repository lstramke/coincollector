package io.github.lstramke.coincollector.model;

/**
 * Fluent builder for {@link EuroCoin}. Performs validation and generates a
 * default description and id when not provided.
 * A pre-existing id can be injected by {@link EuroCoinFactory} via the
 * package-private {@code setId(String)} to preserve identifiers during
 * hydration/import; otherwise an id is generated deterministically from core
 * attributes.
 */
public class EuroCoinBuilder {
    int year;
    CoinValue value;
    CoinCountry mintCountry;
    CoinDescription description;
    Mint mint;
    String id;
    String collectionId;

    public static final int EURO_COIN_START_YEAR = 1999;

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

    public EuroCoinBuilder setCollectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    /**
     * Explicitly set the coin id. Intended for {@link EuroCoinFactory} and
     * persistence/import layers to preserve existing ids; not public by design.
     */
    EuroCoinBuilder setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Validate and build the coin instance.
     * @return new EuroCoin
     * @throws IllegalStateException if mandatory fields missing or invalid
     */
    public EuroCoin build() throws IllegalStateException {
        if (year < EURO_COIN_START_YEAR) {
            throw new IllegalStateException("Year must be >= " + EURO_COIN_START_YEAR);
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
        if (collectionId == null){
            throw new IllegalStateException("CollectionId cannot be null");
        }
        if (id == null){
            this.id = generateId();
        }
        
        return new EuroCoin(this);
    }

    /** Generate a deterministic id based on core attributes. */
    private String generateId() {
        return String.format("%s_%s_%d_%s", 
            mintCountry != null ? mintCountry.name() : "UNKNOWN",
            value != null ? value.name() : "UNKNOWN", 
            year,
            mint != null ? mint.name() : "UNKNOWN"
        );
    }
}
