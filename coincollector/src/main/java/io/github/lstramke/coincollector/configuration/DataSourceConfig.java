package io.github.lstramke.coincollector.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource(@Value("${coincollector.db-file}") String dbFilePath) {
        var sqlite = new SQLiteDataSource();
        sqlite.setUrl("jdbc:sqlite:" + dbFilePath);
        return new DataSourceAutoActivateForeignKeys(sqlite);
    }
}
