package io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException;

public class EuroCoinCollectionGroupGetByIdException extends RuntimeException {
    private static final String ERROR_CODE = "GROUP_NOT_FOUND";

    public EuroCoinCollectionGroupGetByIdException(String groupId, Throwable cause) {
        super("Euro coin collection group with id " + groupId + " not found", cause);
    }

    public EuroCoinCollectionGroupGetByIdException(String groupId) {
        super("Euro coin collection group with id " + groupId + " not found");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
