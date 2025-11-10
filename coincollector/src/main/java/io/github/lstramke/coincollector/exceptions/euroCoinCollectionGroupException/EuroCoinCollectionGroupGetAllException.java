package io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException;

public class EuroCoinCollectionGroupGetAllException extends Exception {
    private static final String ERROR_CODE = "GROUP_GET_ALL_FAILED";
    private static final String DEFAULT_MESSAGE_TEXT = "Failed to retrieve all EuroCoinCollectionGroups";

    public EuroCoinCollectionGroupGetAllException(Throwable cause) {
        super(DEFAULT_MESSAGE_TEXT, cause);
    }

    public EuroCoinCollectionGroupGetAllException() {
        super(DEFAULT_MESSAGE_TEXT);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
