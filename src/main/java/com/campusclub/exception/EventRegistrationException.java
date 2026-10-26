package com.campusclub.exception;

/**
 * Checked exception thrown when an event registration violates business rules,
 * such as event capacity exhaustion, expired registration deadlines, or duplicate signups.
 */
public class EventRegistrationException extends Exception {
    private static final long serialVersionUID = 1L;

    public EventRegistrationException(String message) {
        super(message);
    }

    public EventRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventRegistrationException(Throwable cause) {
        super(cause);
    }
}
