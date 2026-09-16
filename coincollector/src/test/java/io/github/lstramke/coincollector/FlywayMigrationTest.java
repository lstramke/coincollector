package io.github.lstramke.coincollector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlywayMigrationTest {

    @Test
    void migratesExistingCoinsIntoTypesAndKeepsCoinInstances(@TempDir Path temporaryDirectory) throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + temporaryDirectory.resolve("migration.db") + "?foreign_keys=on";

        flyway(jdbcUrl, "1").migrate();
        insertV1Fixtures(jdbcUrl);

        flyway(jdbcUrl, "2").migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            assertEquals(2, count(statement, "SELECT COUNT(*) FROM euroCoins"));
            assertEquals(2, count(statement, "SELECT COUNT(*) FROM euroCoinTypes"));
            assertEquals(2, count(statement, "SELECT COUNT(DISTINCT coin_id) FROM euroCoins"));
            assertEquals(2, count(statement, "SELECT COUNT(DISTINCT type_id) FROM euroCoins"));
            assertEquals(2, count(statement, "SELECT COUNT(*) FROM euroCoins WHERE description IS NOT NULL"));

            Set<String> coinIds = new HashSet<>();
            try (ResultSet resultSet = statement.executeQuery("SELECT coin_id FROM euroCoins")) {
                while (resultSet.next()) {
                    coinIds.add(resultSet.getString("coin_id"));
                }
            }
            assertEquals(2, coinIds.size());
            coinIds.forEach(coinId -> assertDoesNotThrow(() -> UUID.fromString(coinId)));
            assertNotNull(singleString(statement, "SELECT type_id FROM euroCoins LIMIT 1"));
            assertEquals(1, count(statement,
                "SELECT COUNT(*) FROM euroCoinTypes WHERE type_id = 'GERMANY_TWO_EURO_2024_A'"));
            assertEquals(1, count(statement,
                "SELECT COUNT(*) FROM euroCoinTypes WHERE type_id = 'FRANCE_ONE_EURO_2023_B'"));
        }
    }

    private static Flyway flyway(String jdbcUrl, String target) {
        var configuration = Flyway.configure()
            .dataSource(jdbcUrl, null, null)
            .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private static void insertV1Fixtures(String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (user_id, username) VALUES ('user-1', 'migration-test')");
            statement.executeUpdate("INSERT INTO euroCoinCollectionGroups (group_id, name, owner_id) "
                + "VALUES ('group-1', 'migration-group', 'user-1')");
            statement.executeUpdate("INSERT INTO euroCoinCollections (collection_id, name, group_id) "
                + "VALUES ('collection-1', 'migration-collection', 'group-1')");
            statement.executeUpdate("INSERT INTO euroCoins "
                + "(coin_id, year, coin_value, mint_country, mint, description, collection_id) "
                + "VALUES ('GERMANY_TWO_EURO_2024_A', 2024, 200, 'DE', 'A', 'first coin', 'collection-1')");
            statement.executeUpdate("INSERT INTO euroCoins "
                + "(coin_id, year, coin_value, mint_country, mint, description, collection_id) "
                + "VALUES ('FRANCE_ONE_EURO_2023_B', 2023, 100, 'FR', 'B', 'second coin', 'collection-1')");
        }
    }

    private static long count(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static String singleString(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}