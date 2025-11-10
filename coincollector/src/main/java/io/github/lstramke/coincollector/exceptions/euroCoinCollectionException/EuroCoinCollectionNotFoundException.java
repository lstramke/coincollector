package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionNotFoundException extends RuntimeException {

    private static final String ERROR_CODE = "COLLECTION_NOT_FOUND";

    public EuroCoinCollectionNotFoundException(String collectionId, Throwable cause) {
        super("Euro coin collection with id " + collectionId + " not found", cause);
    }

    public EuroCoinCollectionNotFoundException(String collectionId) {
        super("Euro coin collection with id " + collectionId + " not found");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
