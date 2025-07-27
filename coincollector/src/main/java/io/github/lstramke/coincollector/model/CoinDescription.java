package io.github.lstramke.coincollector.model;

public class CoinDescription {
    private String text;

    public CoinDescription(String text){
        this.text = text;
    }

    public CoinDescription(CoinValue value, int year, CoinCountry country, Mint mint) throws IllegalArgumentException{
        if (value == null) throw new IllegalArgumentException("CoinValue cannot be null");
        if (country == null) throw new IllegalArgumentException("CoinCountry cannot be null");
        if (year <= 0) throw new IllegalArgumentException("Year must be greater than 0");
        
        this.text = getGermanCoinDescriptionText(value, year, country, mint);
    }

    private String getGermanCoinDescriptionText(CoinValue value, int year, CoinCountry country, Mint mint) {
        String valueText = value.getDisplayName();
        String countryText = country.getDisplayName();
        String mintText = mint != null ? " aus der Prägestätte " + mint.name() : "";
        
        return String.format("%s Münze aus %s vom Jahr %d%s", valueText, countryText, year, mintText);
    }

    @Override
    public String toString() {
        return "CoinDescription[text=" + text + "]";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
