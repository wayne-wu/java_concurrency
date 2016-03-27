package com.yahoo.javatraining.project2;

/**
 * This is the main exception thrown by the TicketManager library.
 */
public class TicketManagerException extends Exception {
    /**
     * Constructs an exception with just message.
     * @param message Non-null exception messsage.
     */
    public TicketManagerException(String message) {
        super(message);
    }

    /**
     * Wraps another exception.
     * @param cause Non-null exception.
     */
    public TicketManagerException(Throwable cause) {
        super(cause);
    }

    /**
     * Wraps another exception with a more detailed exception message.
     * @param message Non-null exception message.
     * @param cause Non-null exception.
     */
    public TicketManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
