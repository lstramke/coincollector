package io.github.lstramke.coincollector.initializer;

import io.github.lstramke.coincollector.exceptions.StorageException;

public interface StorageInitializer {
    void init() throws StorageException;
}