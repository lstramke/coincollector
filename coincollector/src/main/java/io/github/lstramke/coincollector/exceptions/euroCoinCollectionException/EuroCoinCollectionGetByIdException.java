package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionGetByIdException extends RuntimeException {
    
    private static final String ERROR_CODE = "COLLECTION_GET_BY_ID_FAILED";

    public EuroCoinCollectionGetByIdException(String collectionId, Throwable cause) {
        super("Reading euro coin collection with id " + collectionId + "failed" , cause);
    }

    public EuroCoinCollectionGetByIdException(String collectionId) {
        super("Reading euro coin collection with id " + collectionId + "failed");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
