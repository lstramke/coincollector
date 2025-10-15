package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionCoinsLoadException extends RuntimeException {

    private static final String ERROR_CODE = "LOAD_ALL_COINS_FROM_COLLECTION_FAILED";

    public EuroCoinCollectionCoinsLoadException(String collectionId, Throwable cause) {
        super("Failed to retrieve all EuroCoins in collection " + collectionId, cause);
    }

    public EuroCoinCollectionCoinsLoadException(String collectionId) {
        super("Failed to retrieve all EuroCoins in collection " + collectionId);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
