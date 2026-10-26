package com.campusclub.exception;

/**
 * Checked exception thrown when a student attempts to apply for or join a club
 * in which they already hold an active, pending, or approved membership.
 */
public class DuplicateMembershipException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateMembershipException(String message) {
        super(message);
    }

    public DuplicateMembershipException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateMembershipException(Throwable cause) {
        super(cause);
    }
}
