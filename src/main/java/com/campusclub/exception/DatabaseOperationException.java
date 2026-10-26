package com.campusclub.exception;

/**
 * Unchecked exception thrown when an underlying database / JDBC operation encounters
 * an unrecoverable failure (e.g., connection drop, SQL syntax error, constraint failure).
 * Wrapping SQLExceptions in an unchecked domain exception prevents JDBC technicalities
 * from polluting high-level business contracts while preserving root causes for diagnosis.
 */
public class DatabaseOperationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseOperationException(Throwable cause) {
        super(cause);
    }
}
