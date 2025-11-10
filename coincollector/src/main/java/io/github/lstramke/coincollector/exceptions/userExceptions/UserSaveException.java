package io.github.lstramke.coincollector.exceptions.userExceptions;

public class UserSaveException extends RuntimeException {

    private static final String ERROR_CODE = "USER_SAVE_FAILED";

    public UserSaveException(String userId, Throwable cause) {
        super("User with id " + userId + " not saved", cause);
    }

    public UserSaveException(String userId) {
        super("User with id " + userId + " not saved");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
