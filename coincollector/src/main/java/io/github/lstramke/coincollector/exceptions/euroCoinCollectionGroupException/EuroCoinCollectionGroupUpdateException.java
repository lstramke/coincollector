package io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException;

public class EuroCoinCollectionGroupUpdateException extends Exception {
    
    private static final String ERROR_CODE = "GROUP_UPDATE_FAILED";

    public EuroCoinCollectionGroupUpdateException(String groupId, Throwable cause) {
        super("Euro coin collection group with id " + groupId + " not updated", cause);
    }

    public EuroCoinCollectionGroupUpdateException(String groupId) {
        super("Euro coin collection group with id " + groupId + " not updated");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
