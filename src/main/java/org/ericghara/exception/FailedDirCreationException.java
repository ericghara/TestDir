package org.ericghara.exception;

public class FailedDirCreationException extends RuntimeException {
    public FailedDirCreationException() {
        super();
    }

    public FailedDirCreationException(String message) {
        super(message);
    }

    public FailedDirCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FailedDirCreationException(Throwable cause) {
        super(cause);
    }

    protected FailedDirCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
