package io.github.lstramke.coincollector.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CoinValue {
    ONE_CENT(1, "1 Cent"),
    TWO_CENTS(2, "2 Cent"),
    FIVE_CENTS(5, "5 Cent"),
    TEN_CENTS(10, "10 Cent"),
    TWENTY_CENTS(20, "20 Cent"),
    FIFTY_CENTS(50, "50 Cent"),
    ONE_EURO(100, "1 Euro"),
    TWO_EUROS(200, "2 Euro");

    private final int centValue;
    private final String displayName;

    private static final Map<Integer, CoinValue> VALUE_MAP =
        Stream.of(values()).collect(Collectors.toMap(CoinValue::getCentValue, coin -> coin));

    CoinValue(int centValue, String displayName) {
        this.centValue = centValue;
        this.displayName = displayName;
    }

    public int getCentValue() {
        return centValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CoinValue fromCentValue(int centValue) {
        return VALUE_MAP.get(centValue);
    }
}
