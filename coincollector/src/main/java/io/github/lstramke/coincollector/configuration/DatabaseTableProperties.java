package io.github.lstramke.coincollector.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coincollector.table")
public record DatabaseTableProperties(
    String user,
    String euroCoin,
    String euroCoinCollection,
    String euroCoinCollectionGroup
){}
