package server.application.exceptions;

public class NoSuchStorageException extends Exception {
    public NoSuchStorageException(final String message) {
        super(message);
    }
}
