package io.github.lstramke.coincollector.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * German mints identified by their mint mark letters.
 */
public enum Mint {
    BERLIN("A"),
    MUNICH("D"),
    STUTTGART("F"),
    KARLSRUHE("G"),
    HAMBURG("J"),
    UNKOWN("UNKNOWN");

    private final String mintMark;

    private static final Map<String, Mint> MINT_MAP =
        Stream.of(values()).collect(Collectors.toMap(Mint::getMintMark, mint -> mint));

    Mint(String mintMark) {
        this.mintMark = mintMark;
    }

    @Override
    public String toString() {
        return this.mintMark;
    }

    public String getMintMark() {
        return mintMark;
    }

    /**
     * Lookup by mint mark letter.
     * @param mintMark letter
     * @return mint enum if known, UNKOWN else
     */
    public static Mint fromMintMark(String mintMark) throws IllegalArgumentException {
        Mint mint =  MINT_MAP.get(mintMark);
        if (mint == null){
            return UNKOWN;
        }
        return mint;
    }
}