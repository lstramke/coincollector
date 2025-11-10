package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

public class EuroCoinGetAllException extends RuntimeException {
    
    private static final String ERROR_CODE = "COIN_GET_ALL_FAILED";
    private static final String DEFAULT_MESSAGE_TEXT = "Failed to retrieve all EuroCoins";

    public EuroCoinGetAllException(Throwable cause) {
        super(DEFAULT_MESSAGE_TEXT, cause);
    }

    public EuroCoinGetAllException(){
        super(DEFAULT_MESSAGE_TEXT);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
