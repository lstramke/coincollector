package io.github.lstramke.coincollector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class LoggingConfigurationTest {

    @Test
    void defaultLoggingLevelShouldBeInfo() {
        Properties properties = loadProperties("application.yml");

        assertEquals("INFO", properties.getProperty("logging.level.root"));
    }

    @Test
    void developmentLoggingLevelShouldBeDebug() {
        Properties properties = loadProperties("application-dev.yml");

        assertEquals("DEBUG", properties.getProperty("logging.level.root"));
    }

    private Properties loadProperties(String resourcePath) {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new ClassPathResource(resourcePath));
        Properties properties = yamlPropertiesFactoryBean.getObject();
        assertNotNull(properties);
        return properties;
    }
}
