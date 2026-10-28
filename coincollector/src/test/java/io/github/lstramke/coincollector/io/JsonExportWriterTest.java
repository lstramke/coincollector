package io.github.lstramke.coincollector.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.lstramke.coincollector.export.ExportFormat;
import io.github.lstramke.coincollector.model.CoinCountry;
import io.github.lstramke.coincollector.model.CoinValue;
import io.github.lstramke.coincollector.model.EuroCoinBuilder;
import io.github.lstramke.coincollector.model.EuroCoinCollection;
import io.github.lstramke.coincollector.model.EuroCoinCollectionGroup;
import io.github.lstramke.coincollector.model.Mint;
import tools.jackson.databind.ObjectMapper;

public class JsonExportWriterTest {

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

    private record JsonExportTestcase(
        List<EuroCoinCollectionGroup> collection,
        byte[] expectedContent,
        String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    private static Stream<JsonExportTestcase> csvExportTestcases() {
        return Stream.of(
            new JsonExportTestcase(
                List.of(),
                "[]".getBytes(StandardCharsets.UTF_8),
                "empty collection"
            ),
            new JsonExportTestcase(
                groups,
                """
                [
                  {
                    "name": "dummyGroup",
                    "collections": [
                      {
                        "name": "dummy collection",
                        "coins": [
                          {
                            "year": 2020,
                            "value": "50 Cent",
                            "mintCountry": "Belgien",
                            "mint": "UNKNOWN",
                            "description": "50 Cent Münze aus Belgien aus dem Jahr 2020 aus der Prägestätte UNKNOWN"
                          },
                          {
                            "year": 2021,
                            "value": "2 Cent",
                            "mintCountry": "Frankreich",
                            "mint": "UNKNOWN",
                            "description": "2 Cent Münze aus Frankreich aus dem Jahr 2021 aus der Prägestätte UNKNOWN"
                          },
                          {
                            "year": 2022,
                            "value": "2 Euro",
                            "mintCountry": "Italien",
                            "mint": "UNKNOWN",
                            "description": "2 Euro Münze aus Italien aus dem Jahr 2022 aus der Prägestätte UNKNOWN"
                          }
                        ]
                      },
                      {
                        "name": "dummy collection 2",
                        "coins": [
                          {
                            "year": 2018,
                            "value": "1 Euro",
                            "mintCountry": "Estland",
                            "mint": "UNKNOWN",
                            "description": "1 Euro Münze aus Estland aus dem Jahr 2018 aus der Prägestätte UNKNOWN"
                          },
                          {
                            "year": 2021,
                            "value": "2 Cent",
                            "mintCountry": "Frankreich",
                            "mint": "UNKNOWN",
                            "description": "2 Cent Münze aus Frankreich aus dem Jahr 2021 aus der Prägestätte UNKNOWN"
                          },
                          {
                            "year": 2006,
                            "value": "20 Cent",
                            "mintCountry": "Italien",
                            "mint": "UNKNOWN",
                            "description": "20 Cent Münze aus Italien aus dem Jahr 2006 aus der Prägestätte UNKNOWN"
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "name": "dummyGroup2",
                    "collections": [
                      {
                        "name": "dummy collection 3",
                        "coins": [
                          {
                            "year": 2018,
                            "value": "1 Euro",
                            "mintCountry": "Deutschland",
                            "mint": "A",
                            "description": "1 Euro Münze aus Deutschland aus dem Jahr 2018 aus der Prägestätte A"
                          },
                          {
                            "year": 2021,
                            "value": "2 Cent",
                            "mintCountry": "Deutschland",
                            "mint": "D",
                            "description": "2 Cent Münze aus Deutschland aus dem Jahr 2021 aus der Prägestätte D"
                          },
                          {
                            "year": 2006,
                            "value": "20 Cent",
                            "mintCountry": "Italien",
                            "mint": "UNKNOWN",
                            "description": "20 Cent Münze aus Italien aus dem Jahr 2006 aus der Prägestätte UNKNOWN"
                          }
                        ]
                      }
                    ]
                  }
                ]
                """.strip().getBytes(StandardCharsets.UTF_8),
                "two groups with coins"
            )
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("csvExportTestcases")
    void testWrite(JsonExportTestcase testcase) {
        ObjectMapper mapper = new ObjectMapper();
        JsonExportWriter writer = new JsonExportWriter(mapper);

        assertEquals(ExportFormat.JSON, writer.format());

        byte[] result = assertDoesNotThrow(
            () -> writer.write(testcase.collection())
        );

        assertArrayEquals(
            testcase.expectedContent(),
            result, new String(result, StandardCharsets.UTF_8)
        );
    }
}