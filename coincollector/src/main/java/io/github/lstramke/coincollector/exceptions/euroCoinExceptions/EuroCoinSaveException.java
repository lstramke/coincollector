package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

import java.util.UUID;

public class EuroCoinSaveException extends RuntimeException{

    private static final String ERROR_CODE = "COIN_SAVE_FAILED";

     public EuroCoinSaveException(UUID coinId, Throwable cause) {
        super("Euro coin with id " + coinId + " not saved", cause);
    }

    public EuroCoinSaveException(UUID coinId){
        super("Euro coin with id " + coinId + " not saved");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}