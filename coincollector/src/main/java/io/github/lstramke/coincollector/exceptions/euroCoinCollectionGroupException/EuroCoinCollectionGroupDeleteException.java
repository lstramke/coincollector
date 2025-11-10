package io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException;

public class EuroCoinCollectionGroupDeleteException extends Exception {
    
    private static final String ERROR_CODE = "GROUP_DELETE_FAILED";

    public EuroCoinCollectionGroupDeleteException(String groupId, Throwable cause) {
        super("Euro coin collection group with id " + groupId + " not deleted", cause);
    }

    public EuroCoinCollectionGroupDeleteException(String groupId) {
        super("Euro coin collection group with id " + groupId + " not deleted");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
