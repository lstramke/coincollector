package io.github.lstramke.coincollector.exceptions.euroCoinCollectionGroupException;

public class EuroCoinCollectionGroupSaveException extends Exception {

    private static final String ERROR_CODE = "GROUP_SAVE_FAILED";

    public EuroCoinCollectionGroupSaveException(String groupId, Throwable cause) {
        super("Euro coin collection group with id " + groupId + " not saved", cause);
    }

    public EuroCoinCollectionGroupSaveException(String groupId) {
        super("Euro coin collection group with id " + groupId + " not saved");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
