package io.github.lstramke.coincollector.exceptions.euroCoinExceptions;

public class EuroCoinDeleteException extends RuntimeException {
    
    private static final String ERROR_CODE = "COIN_DELETE_FAILED";

    public EuroCoinDeleteException(String coinId, Throwable cause) {
        super("Euro coin with id " + coinId + " not deleted", cause);
    }

    public EuroCoinDeleteException(String coinId){
        super("Euro coin with id " + coinId + " not deleted");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
