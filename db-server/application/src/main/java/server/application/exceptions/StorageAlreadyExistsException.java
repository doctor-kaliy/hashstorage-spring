package server.application.exceptions;

public class StorageAlreadyExistsException extends Exception {
    public StorageAlreadyExistsException(final String message) {
        super(message);
    }
}
