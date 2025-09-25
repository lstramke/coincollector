package io.github.lstramke.coincollector.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CoinCountry {
    AUSTRIA("AT", "Österreich"),
    BELGIUM("BE", "Belgien"),
    CYPRUS("CY", "Zypern"),
    GERMANY("DE", "Deutschland"),
    ESTONIA("EE", "Estland"),
    SPAIN("ES", "Spanien"),
    FINLAND("FI", "Finnland"),
    FRANCE("FR", "Frankreich"),
    GREECE("GR", "Griechenland"),
    IRELAND("IE", "Irland"),
    ITALY("IT", "Italien"),
    LITHUANIA("LT", "Litauen"),
    LUXEMBOURG("LU", "Luxemburg"),
    LATVIA("LV", "Lettland"),
    MALTA("MT", "Malta"),
    NETHERLANDS("NL", "Niederlande"),
    PORTUGAL("PT", "Portugal"),
    SLOVENIA("SI", "Slowenien"),
    SLOVAKIA("SK", "Slowakei"),
    SAN_MARINO("SM", "San Marino"),
    VATICAN_CITY("VA", "Vatikanstadt"),
    MONACO("MC", "Monaco"),
    ANDORRA("AD", "Andorra");

    private final String isoCode;
    private final String displayName;

    private static final Map<String, CoinCountry> CODE_MAP =
        Stream.of(values()).collect(Collectors.toMap(CoinCountry::getIsoCode, country -> country));

    CoinCountry(String isoCode, String displayName) {
        this.isoCode = isoCode;
        this.displayName = displayName;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CoinCountry fromIsoCode(String code) throws IllegalArgumentException{
        CoinCountry country = CODE_MAP.get(code);
        if (country == null) {
            throw new IllegalArgumentException("Unknown ISO code: " + code);
        }
        return country;
    }

}
