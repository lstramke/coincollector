package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

public class EuroCoinUpdateException extends RuntimeException {

    private static final String ERROR_CODE = "COIN_UPDATE_FAILED";

     public EuroCoinUpdateException(String coinId, Throwable cause) {
        super("Euro coin with id " + coinId + " not updated", cause);
    }

    public EuroCoinUpdateException(String coinId){
        super("Euro coin with id " + coinId + " not updated");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
