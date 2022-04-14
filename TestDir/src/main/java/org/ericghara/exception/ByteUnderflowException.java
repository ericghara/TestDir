package org.ericghara.exception;

public class ByteUnderflowException extends RuntimeException {


    public ByteUnderflowException() {
        super();
    }

    public ByteUnderflowException(String message) {
        super(message);
    }

    public ByteUnderflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public ByteUnderflowException(Throwable cause) {
        super(cause);
    }

    protected ByteUnderflowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
