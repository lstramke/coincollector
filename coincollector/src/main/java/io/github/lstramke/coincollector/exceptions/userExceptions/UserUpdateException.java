package io.github.lstramke.coincollector.exceptions.userExceptions;

public class UserUpdateException extends RuntimeException {

    private static final String ERROR_CODE = "USER_UPDATE_FAILED";

    public UserUpdateException(String userId, Throwable cause) {
        super("User with id " + userId + " not updated", cause);
    }

    public UserUpdateException(String userId) {
        super("User with id " + userId + " not updated");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
