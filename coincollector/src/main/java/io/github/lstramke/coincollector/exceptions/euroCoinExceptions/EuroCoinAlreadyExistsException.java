package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

import java.util.UUID;

public class EuroCoinAlreadyExistsException extends RuntimeException {

    private static final String ERROR_CODE = "COIN_EXISTS_ALREADY";

    public EuroCoinAlreadyExistsException(UUID coinId){
        super("Euro coin with id " + coinId + "already exists");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
