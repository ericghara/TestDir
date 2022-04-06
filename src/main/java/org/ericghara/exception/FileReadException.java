package org.ericghara.exception;

public class FileReadException extends RuntimeException {

    public FileReadException() {
        super();
    }

    public FileReadException(String message) {
        super(message);
    }

    public FileReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileReadException(Throwable cause) {
        super(cause);
    }

    protected FileReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
