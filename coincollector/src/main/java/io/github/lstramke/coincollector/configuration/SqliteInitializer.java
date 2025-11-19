package io.github.lstramke.coincollector.configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lstramke.coincollector.exceptions.StorageInitializeException;

public class SqliteInitializer implements StorageInitializer{
    private final DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(SqliteInitializer.class);
    private final List<String> tableNames;

    public SqliteInitializer(DataSource dataSource, List<String> tableNames) {
        this.dataSource = dataSource;
        this.tableNames = tableNames;
    }

    @Override
    public void init() throws StorageInitializeException  {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                initUserTable(connection);
                initEuroCoinCollectionGroupTable(connection);
                initEuroCoinCollectionTable(connection);
                initEuroCoinTable(connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                logger.error("Transaction rolled back due to error: {}", e.getMessage());
                throw new StorageInitializeException("Failed to initialize tables: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize database: {}", e.getMessage());
            throw new StorageInitializeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    private void initUserTable(Connection connection) throws StorageInitializeException  {
        String tableName = tableNames.get(0);
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                user_id TEXT PRIMARY KEY,
                username TEXT UNIQUE NOT NULL
            )
            """, tableName);
        initTable(connection, tableName, sql);
    }

    private void initEuroCoinCollectionGroupTable(Connection connection) throws StorageInitializeException {
        String tableName = tableNames.get(1);
        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    group_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE,
                    owner_id TEXT NOT NULL,
                    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """, tableName);
        initTable(connection, tableName, sql);
    }

    private void initEuroCoinCollectionTable(Connection connection) throws StorageInitializeException {
        String tableName = tableNames.get(2);
        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    collection_id TEXT PRIMARY KEY,
                    name TEXT NOT NULL UNIQUE,
                    group_id TEXT NOT NULL,
                    FOREIGN KEY (group_id) REFERENCES euroCoinCollectionGroups(group_id) ON DELETE CASCADE
                )
                """, tableName);
        initTable(connection, tableName, sql);
    }
    
    private void initEuroCoinTable(Connection connection) throws StorageInitializeException {
        String tableName = tableNames.get(3);
        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    coin_id TEXT PRIMARY KEY,
                    year INTEGER NOT NULL,
                    coin_value INTEGER NOT NULL,
                    mint_country TEXT NOT NULL,
                    mint TEXT,
                    description TEXT NOT NULL,
                    collection_id TEXT NOT NULL,
                    FOREIGN KEY (collection_id) REFERENCES euroCoinCollections(collection_id) ON DELETE CASCADE
                )
                """, tableName);
        initTable(connection, tableName, sql);
    }

    private void initTable(Connection connection, String name, String sql) throws StorageInitializeException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            logger.info("Table {} initialized successfully", name);
        } catch (SQLException e) {
            logger.error("Failed to initialize table {}", name, e);
            throw new StorageInitializeException("Failed to initialize table '" + name + "': " + e.getMessage(), e);
        }
    }
}