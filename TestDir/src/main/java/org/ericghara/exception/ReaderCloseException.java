package org.ericghara.exception;

public class ReaderCloseException extends RuntimeException {

    public ReaderCloseException() {
        super();
    }

    public ReaderCloseException(String message) {
        super(message);
    }

    public ReaderCloseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReaderCloseException(Throwable cause) {
        super(cause);
    }

    protected ReaderCloseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
