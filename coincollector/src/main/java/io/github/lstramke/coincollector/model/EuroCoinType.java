package io.github.lstramke.coincollector.model;

public record EuroCoinType(
    String id,
    int year,
    CoinValue value,
    CoinCountry mintCountry,
    Mint mint
) {
    public EuroCoinType(
        int year,
        CoinValue value,
        CoinCountry mintCountry,
        Mint mint
    ) {
        this(
            generateId(mintCountry, value, year, mint),
            year,
            value,
            mintCountry,
            mint
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EuroCoinType other = (EuroCoinType) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    private static String generateId(
        CoinCountry mintCountry,
        CoinValue value,
        int year,
        Mint mint
    ) {
        return String.format(
            "%s_%s_%d_%s",
            mintCountry != null ? mintCountry.name() : "UNKNOWN",
            value != null ? value.name() : "UNKNOWN",
            year,
            mint != null ? mint.name() : "UNKNOWN"
        );
    }
}
