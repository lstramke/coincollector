package io.github.lstramke.coincollector.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CoinValue {
    ONE_CENT(1),
    TWO_CENTS(2),
    FIVE_CENTS(5),
    TEN_CENTS(10),
    TWENTY_CENTS(20),
    FIFTY_CENTS(50),
    ONE_EURO(100),
    TWO_EUROS(200);

    private final int centValue;

    private static final Map<Integer, CoinValue> VALUE_MAP =
        Stream.of(values()).collect(Collectors.toMap(CoinValue::getCentValue, coin -> coin));

    CoinValue(int centValue) {
        this.centValue = centValue;
    }

    public int getCentValue() {
        return centValue;
    }

    public static CoinValue fromCentValue(int centValue) {
        return VALUE_MAP.get(centValue);
    }
}
