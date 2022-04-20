package org.ericghara.exception;

/**
 * An exception during file creation.
 */
public class FileCreationException extends RuntimeException {

    public FileCreationException() {
        super();
    }

    public FileCreationException(String message) {
        super(message);
    }

    public FileCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileCreationException(Throwable cause) {
        super(cause);
    }

    protected FileCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
