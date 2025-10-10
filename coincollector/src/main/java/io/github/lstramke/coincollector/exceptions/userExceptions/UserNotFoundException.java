package io.github.lstramke.coincollector.exceptions.userExceptions;

public class UserNotFoundException extends RuntimeException {

    private static final String ERROR_CODE = "USER_NOT_FOUND";

    public UserNotFoundException(String userIdentifier, Throwable cause) {
       super("User '" + userIdentifier + "' not found", cause);
    }

    public UserNotFoundException(String userIdentifier) {
        super("User '" + userIdentifier + "' not found");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
