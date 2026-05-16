package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

import java.util.UUID;

public class EuroCoinNotFoundException extends RuntimeException {

    private static final String ERROR_CODE = "COIN_NOT_FOUND";

    public EuroCoinNotFoundException(UUID coinId, Throwable cause) {
        super("Euro coin with id " + coinId + " not found", cause);
    }

    public EuroCoinNotFoundException(UUID coinId){
        super("Euro coin with id " + coinId + " not found");
    }

    public String getErrorCode(){
        return ERROR_CODE;
    }
}
