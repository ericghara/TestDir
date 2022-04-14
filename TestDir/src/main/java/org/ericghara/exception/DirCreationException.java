package org.ericghara.exception;

public class DirCreationException extends RuntimeException {
    public DirCreationException() {
        super();
    }

    public DirCreationException(String message) {
        super(message);
    }

    public DirCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirCreationException(Throwable cause) {
        super(cause);
    }

    protected DirCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
