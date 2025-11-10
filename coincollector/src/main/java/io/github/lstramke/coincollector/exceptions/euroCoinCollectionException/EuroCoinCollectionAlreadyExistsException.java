package io.github.lstramke.coincollector.exceptions.euroCoinCollectionException;

public class EuroCoinCollectionAlreadyExistsException extends RuntimeException {
    
    private static final String ERROR_CODE = "Collection_EXISTS_ALREADY";

    public EuroCoinCollectionAlreadyExistsException(String collectionId){
        super("Euro coin collection with id " + collectionId + "already exists");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
