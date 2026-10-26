package com.campusclub.exception;

/**
 * Checked exception thrown when an operation targets a club ID or entity
 * that does not exist in the database.
 */
public class ClubNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public ClubNotFoundException(String message) {
        super(message);
    }

    public ClubNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClubNotFoundException(Throwable cause) {
        super(cause);
    }
}
