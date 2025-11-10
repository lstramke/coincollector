package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionDeleteException extends RuntimeException {

    private static final String ERROR_CODE = "COLLECTION_DELETE_FAILED";

    public EuroCoinCollectionDeleteException(String collectionId, Throwable cause) {
        super("Euro coin collection with id " + collectionId + " not deleted", cause);
    }

    public EuroCoinCollectionDeleteException(String collectionId) {
        super("Euro coin collection with id " + collectionId + " not deleted");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
