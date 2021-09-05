package server.application.exceptions;

public class InvalidStorageNameException extends Exception {
    public InvalidStorageNameException(final String message) {
        super(message);
    }
}
