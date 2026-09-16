package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

import java.util.UUID;

public class EuroCoinDeleteException extends RuntimeException {
    
    private static final String ERROR_CODE = "COIN_DELETE_FAILED";

    public EuroCoinDeleteException(UUID coinId, Throwable cause) {
        super("Euro coin with id " + coinId + " not deleted", cause);
    }

    public EuroCoinDeleteException(UUID coinId){
        super("Euro coin with id " + coinId + " not deleted");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
