package org.ericghara.exception;

/**
 * An exception while performing a filesystem write.
 */
public class WriteFailureException extends RuntimeException {

    public WriteFailureException() {
        super();
    }

    public WriteFailureException(String message) {
        super(message);
    }

    public WriteFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriteFailureException(Throwable cause) {
        super(cause);
    }

    protected WriteFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
