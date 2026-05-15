package io.github.lstramke.coincollector.configuration;

import io.github.lstramke.coincollector.exceptions.StorageInitializeException;

@Deprecated
public interface StorageInitializer {
    @Deprecated
    void init() throws StorageInitializeException ;
}