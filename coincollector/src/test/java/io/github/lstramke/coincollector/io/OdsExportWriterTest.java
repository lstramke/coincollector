package io.github.lstramke.coincollector.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.lstramke.coincollector.export.ExportFormat;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.Mint;

public class OdsExportWriterTest {

    private record CoinData(CoinValue value, CoinCountry country, Mint mint, int year) {}

    private static final EuroCoinCollectionGroup dummyGroup = new EuroCoinCollectionGroup("dummyGroup", "testOwner");
    private static final EuroCoinCollectionGroup dummyGroup2 = new EuroCoinCollectionGroup("dummyGroup2", "owner2");
    private static final List<EuroCoinCollectionGroup> groups = List.of(dummyGroup, dummyGroup2);
    private final static EuroCoinCollection dummyCollection = new EuroCoinCollection("dummy collection", dummyGroup.getId());
    private final static EuroCoinCollection dummyCollection2 = new EuroCoinCollection("dummy collection 2", dummyGroup.getId());
    private final static EuroCoinCollection dummyCollection3 = new EuroCoinCollection("dummy collection 3", dummyGroup2.getId());
    static {
        Stream.of(
            new CoinData(CoinValue.FIFTY_CENTS, CoinCountry.BELGIUM, Mint.UNKOWN, 2020),
            new CoinData(CoinValue.TWO_CENTS, CoinCountry.FRANCE, Mint.UNKOWN, 2021),
            new CoinData(CoinValue.TWO_EUROS, CoinCountry.ITALY, Mint.UNKOWN, 2022)
        )
        .map(data -> new EuroCoinBuilder()
            .setValue(data.value)
            .setYear(data.year)
            .setMintCountry(data.country)
            .setMint(data.mint)
            .setCollectionId(dummyCollection.getId())
            .build())
        .forEach(dummyCollection::addCoin);

        Stream.of(
            new CoinData(CoinValue.ONE_EURO, CoinCountry.ESTONIA, Mint.UNKOWN, 2018),
            new CoinData(CoinValue.TWO_CENTS, CoinCountry.FRANCE, Mint.UNKOWN, 2021),
            new CoinData(CoinValue.TWENTY_CENTS, CoinCountry.ITALY, Mint.UNKOWN, 2006)
        )
        .map(data -> new EuroCoinBuilder()
            .setYear(data.year)
            .setValue(data.value)
            .setMintCountry(data.country)
            .setMint(data.mint)
            .setCollectionId(dummyCollection2.getId())
            .build())
        .forEach(dummyCollection2::addCoin);

        dummyGroup.addCollection(dummyCollection);
        dummyGroup.addCollection(dummyCollection2);

        Stream.of(
            new CoinData(CoinValue.ONE_EURO, CoinCountry.GERMANY, Mint.BERLIN, 2018),
            new CoinData(CoinValue.TWO_CENTS, CoinCountry.GERMANY, Mint.MUNICH, 2021),
            new CoinData(CoinValue.TWENTY_CENTS, CoinCountry.ITALY, Mint.UNKOWN, 2006)
        )
        .map(data -> new EuroCoinBuilder()
            .setYear(data.year)
            .setValue(data.value)
            .setMintCountry(data.country)
            .setMint(data.mint)
            .setCollectionId(dummyCollection3.getId())
            .build())
        .forEach(dummyCollection3::addCoin);

        dummyGroup2.addCollection(dummyCollection3);
    }

    private record OdsExportTestcase(
        List<EuroCoinCollectionGroup> collection,
        List<String> expectedContent,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<OdsExportTestcase> odsExportTestcases() {
        return Stream.of(
            new OdsExportTestcase(
                List.of(),
                List.of(),
                "empty collection"
            ),
            new OdsExportTestcase(
                groups,
                List.of(
                    "table:name=\"dummyGroup\"",
                    "table:name=\"dummyGroup2\"",
                    "dummyGroup",
                    "dummy collection",
                    "dummy collection 2",
                    "dummyGroup2",
                    "dummy collection 3",
                    "50 Cent",
                    "Deutschland",
                    "A",
                    "2 Euro Münze aus Italien aus dem Jahr 2022"
                ),
                "two groups with coins"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("odsExportTestcases")
    void testWrite(OdsExportTestcase testcase) {
        var writer = new OdsExportWriter();

        assertEquals(ExportFormat.ODS, writer.format());

        byte[] result = assertDoesNotThrow(
            () -> writer.write(testcase.collection())
        );

        String content = assertDoesNotThrow(
            () -> readContentXml(result)
        );

        assertTrue(result.length > 0);
        for (String expected : testcase.expectedContent()) {
            assertTrue(
                content.contains(expected),
                () -> "ODS content did not contain: " + expected
            );
        }
    }

    private String readContentXml(byte[] odsContent) throws IOException {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(odsContent))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("content.xml".equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        throw new IOException("ODS archive does not contain content.xml");
    }
}
