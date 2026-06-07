package com.cs.accountsvc.exception;

import java.util.UUID;

/**
 * Raised when an event id is reused with different account transaction data.
 */
public class DuplicateTransactionConflictException extends RuntimeException {

    public DuplicateTransactionConflictException(UUID eventId) {
        super("Transaction event id already exists with a different payload: " + eventId);
    }
}
