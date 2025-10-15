package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionUpdateException extends RuntimeException {

    private static final String ERROR_CODE = "COLLECTION_UPDATE_FAILED";

    public EuroCoinCollectionUpdateException(String collectionId, Throwable cause) {
        super("Euro coin collection with id " + collectionId + " not updated", cause);
    }

    public EuroCoinCollectionUpdateException(String collectionId) {
        super("Euro coin collection with id " + collectionId + " not updated");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
