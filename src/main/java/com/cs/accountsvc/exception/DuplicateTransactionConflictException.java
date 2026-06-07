package com.cs.accountsvc.exception;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * Raised when an event id is reused with different account transaction data.
 *
 * <p>Exact duplicate Event Gateway retries are safe and acknowledged as
 * duplicates. This exception represents the unsafe case where the same event id
 * arrives with a changed account id, type, amount, currency, or event timestamp.
 * The global exception handler maps it to a stable conflict response.</p>
 */
@Slf4j
public class DuplicateTransactionConflictException extends RuntimeException {

    /**
     * Creates a duplicate transaction conflict exception for the reused event id.
     *
     * @param eventId upstream event id that was reused with different payload data
     */
    public DuplicateTransactionConflictException(UUID eventId) {
        super(message(eventId));
    }

    private static String message(UUID eventId) {
        log.warn("Creating duplicate transaction conflict exception eventId={}", eventId);
        return "Transaction event id already exists with a different payload: " + eventId;
    }
}
