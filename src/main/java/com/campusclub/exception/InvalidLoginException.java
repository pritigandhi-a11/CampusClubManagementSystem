package com.campusclub.exception;

/**
 * Checked exception thrown when user authentication fails due to invalid
 * credentials, deactivated accounts, or failed credential verification.
 * Requires the UI/caller to handle and present a user-friendly error message.
 */
public class InvalidLoginException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidLoginException(String message) {
        super(message);
    }

    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLoginException(Throwable cause) {
        super(cause);
    }
}
