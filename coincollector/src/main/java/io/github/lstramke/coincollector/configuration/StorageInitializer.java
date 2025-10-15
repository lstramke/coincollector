package io.github.lstramke.coincollector.configuration;

import io.github.lstramke.coincollector.exceptions.StorageInitializeException;

public interface StorageInitializer {
    void init() throws StorageInitializeException ;
}