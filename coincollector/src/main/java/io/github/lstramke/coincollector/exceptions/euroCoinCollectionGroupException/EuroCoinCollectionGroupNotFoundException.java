package io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException;

public class EuroCoinCollectionGroupNotFoundException extends Exception {
    private static final String ERROR_CODE = "GROUP_NOT_FOUND";

    public EuroCoinCollectionGroupNotFoundException(String groupId, Throwable cause) {
        super("Euro coin collection group with id " + groupId + " not found", cause);
    }

    public EuroCoinCollectionGroupNotFoundException(String groupId) {
        super("Euro coin collection group with id " + groupId + " not found");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
