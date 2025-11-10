package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionSaveException extends RuntimeException {

    private static final String ERROR_CODE = "COLLECTION_SAVE_FAILED";

    public EuroCoinCollectionSaveException(String collectionId, Throwable cause) {
        super("Euro coin collection with id " + collectionId + " not saved", cause);
    }

    public EuroCoinCollectionSaveException(String collectionId) {
        super("Euro coin collection with id " + collectionId + " not saved");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
