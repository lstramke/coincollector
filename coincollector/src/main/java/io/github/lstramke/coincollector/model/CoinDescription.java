package io.github.lstramke.coincollector.model;

/**
 * Simple wrapper encapsulating a textual description for a coin. Provides a
 * convenience constructor to auto-generate a German sentence based on coin
 * metadata.
 */
public class CoinDescription {
    private String text;

    public CoinDescription(String text){
        this.text = text;
    }

    /**
     * Auto-generate German description from coin attributes.
     * @param value coin value
     * @param year mint year (>0)
     * @param country issuing country
     * @param mint optional mint
     * @throws IllegalArgumentException if required argument invalid
     */
    public CoinDescription(CoinValue value, int year, CoinCountry country, Mint mint) throws IllegalArgumentException{
        if (value == null) throw new IllegalArgumentException("CoinValue cannot be null");
        if (country == null) throw new IllegalArgumentException("CoinCountry cannot be null");
        if (year <= 0) throw new IllegalArgumentException("Year must be greater than 0");
        
        this.text = getGermanCoinDescriptionText(value, year, country, mint);
    }

    private String getGermanCoinDescriptionText(CoinValue value, int year, CoinCountry country, Mint mint) {
        String valueText = value.getDisplayName();
        String countryText = country.getDisplayName();
        String mintText = mint != null ? " aus der Prägestätte " + mint.getMintMark() : "";
        
        return String.format("%s Münze aus %s aus dem Jahr %d%s", valueText, countryText, year, mintText);
    }

    @Override
    public String toString() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
