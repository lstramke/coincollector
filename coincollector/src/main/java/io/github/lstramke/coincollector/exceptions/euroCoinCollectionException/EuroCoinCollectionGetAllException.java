package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionGetAllException extends RuntimeException {

    private static final String ERROR_CODE = "COLLECTION_GET_ALL_FAILED";
    private static final String DEFAULT_MESSAGE_TEXT = "Failed to retrieve all EuroCoinCollections";

    public EuroCoinCollectionGetAllException(Throwable cause) {
        super(DEFAULT_MESSAGE_TEXT, cause);
    }

    public EuroCoinCollectionGetAllException() {
        super(DEFAULT_MESSAGE_TEXT);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
