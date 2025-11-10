package io.github.lstramke.coincollector.exceptions.userExceptions;

public class UserDeleteException extends RuntimeException {

    private static final String ERROR_CODE = "USER_DELETE_FAILED";

    public UserDeleteException(String userId, Throwable cause) {
        super("User with id " + userId + " not deleted", cause);
    }

    public UserDeleteException(String userId) {
        super("User with id " + userId + " not deleted");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
