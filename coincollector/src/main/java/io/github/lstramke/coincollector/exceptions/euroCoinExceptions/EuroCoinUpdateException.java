package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

import java.util.UUID;

public class EuroCoinUpdateException extends RuntimeException {

    private static final String ERROR_CODE = "COIN_UPDATE_FAILED";

     public EuroCoinUpdateException(UUID coinId, Throwable cause) {
        super("Euro coin with id " + coinId + " not updated", cause);
    }

    public EuroCoinUpdateException(UUID coinId){
        super("Euro coin with id " + coinId + " not updated");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
